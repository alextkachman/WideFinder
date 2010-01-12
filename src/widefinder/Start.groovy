package widefinder

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.regex.Matcher
import java.util.regex.Pattern


@Typed
class Start
{
    /**
     * Possible HTTP methods:
     * http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html
     */
    private static final String HTTP_METHODS = 'GET|POST|HEAD|PUT|OPTIONS|DELETE|TRACE|CONNECT';


   /**
    * Top N counter
    */
    private static final int N = 10;


    /**
     * Single line pattern
     */
    private static final Pattern PATTERN =
        Pattern.compile( /^(\S+).+?"($HTTP_METHODS) (\S+) HTTP\/1\.(?:1|0)" (\d+) (\S+) "(.+?)"/ );


    /**
     * Array of booleans where only "end of line" indices (10, 13) are set to "true"
     */
    private static final boolean[] BOOLEANS =
        ( 0 .. 255 ).collect{ ( it == ( int ) '\r' ) || ( it == ( int ) '\n' ) }


    public static void main ( String[] args )
    {
        long   t            = System.currentTimeMillis();
        int    bufferSizeMb = 10;
        int    cpuNum       = Runtime.getRuntime().availableProcessors();
        File   file         = new File( args[ 0 ] );
        Stat   stat         = new Stat();

        assert file.isFile();

        int             bufferSize = Math.min( file.size(), ( bufferSizeMb * 1024 * 1024 ));
        ByteBuffer      buffer     = ByteBuffer.allocate( bufferSize );
        FileInputStream fis        = new FileInputStream( file );
        FileChannel     channel    = fis.getChannel();
        long            lines      = processLines( channel, buffer, cpuNum, stat );

        channel.close();
        fis.close();

        Map<String, Long> topArticles = Stat.top( N, stat.articlesToHits());

        report( "Top $N articles (by hits)",          topArticles );
        report( "Top $N URIs (by bytes count)",       Stat.top( N, stat.uriToByteCounts()));
        report( "Top $N URIs (by 404 responses)",     Stat.top( N, stat.uriTo404()));
        report( "Top $N clients (by hot articles)",   Stat.top( N, topArticles, stat.articlesToClients()));
        report( "Top $N referrers (by hot articles)", Stat.top( N, topArticles, stat.articlesToReferrers()));
        
        println "[${ System.currentTimeMillis() - t }] ms"
    }


    static void report( String title, Map<String, Long> map )
    {
        println ">>> $title <<<: \n* ${ map.entrySet().collect{ Map.Entry entry -> "${ entry.key } : ${ entry.value }" }.join( "\n* " ) }"
    }


   /**
    * Reads number of lines in the channel specified
    */
    private static long processLines ( FileChannel channel, ByteBuffer buffer, int cpuNum, Stat stat )
    {
        buffer.rewind();

        long totalLines     = 0;
        long totalBytesRead = 0;

        /**
         * Reading from file channel into buffer (until it ends)
         */
        for ( int remaining = 0; ( channel.position() < channel.size()); )
        {
            int  bytesRead  = channel.read( buffer );
            totalBytesRead += bytesRead;
            byte[] array    = buffer.array();
            boolean isEof   = ( channel.position() == channel.size());

            assert (( bytesRead > 0 ) &&
                        (( bytesRead + remaining ) == buffer.position()) &&
                            ( buffer.position()    <= array.length ));

            /**
             * Iterating through buffer, giving each thread it's own byte[] chunk to analyze:
             *
             * "startIndex" - byte[] index where chunk starts (inclusive)
             * "endIndex"   - byte[] index where chunk ends (exclusive)
             * "chunkSize"  - approximate size of byte[] chunk to be given to each thread             *
             * "chunk"      - array[ startIndex ] - array[ endIndex - 1 ]
             */
            int startIndex = 0;
            int chunkSize  = ( buffer.position() / cpuNum );

           /**
            * When chunk size is too small - we leave only a single chunk for a single thread
            */
            if ( chunkSize < 1024 ) { chunkSize = buffer.position() }

            for ( int endIndex = chunkSize; ( endIndex <= buffer.position()); endIndex += chunkSize )
            {
                if ((( buffer.position() - endIndex ) < chunkSize ) && ( isEof ))
                {
                    /**
                     * We're too close to end of buffer and there will be no more file reads
                     * (that usually collect bytes left from the previous read) - expanding
                     * "endIndex" to the end current buffer
                     */
                    endIndex = buffer.position();
                }
                else
                {
                    /**
                     * Looking for closest "end of line" bytes sequence (that may spread over multiple bytes)
                     * so that array[ endIndex - 1 ] is an *end* of "end of line" bytes sequence
                     */

                    while (( endIndex < buffer.position()) && (   endOfLine( array[ endIndex     ] ))) { endIndex++ }
                    while (( endIndex > 0 )                && ( ! endOfLine( array[ endIndex - 1 ] ))) { endIndex-- }
                }

                assert (( startIndex == 0 ) || (( startIndex > 0 ) && endOfLine( array[ startIndex - 1 ] )));
                assert (                        ( endIndex   > 0 ) && endOfLine( array[ endIndex   - 1 ] ));
                assert (                                    ( ! endOfLine( array[ startIndex ] )));
                assert (( endIndex == buffer.position()) || ( ! endOfLine( array[ endIndex ]   )));

                totalLines += countLines( array, startIndex, endIndex, stat );
                startIndex  = endIndex;
            }

            buffer.position( startIndex );  // Moving buffer's position a little back to last known "endIndex"
            remaining = buffer.remaining(); // How many bytes are left unread in buffer
            buffer.compact();               // Copying remaining (unread) bytes to beginning of buffer
                                            // Next file read will be added to them
        }

        assert ( totalBytesRead == channel.size());
        return totalLines;
    }



   /**
    * This is where each thread gets it's own byte[] chunk to analyze:
    * - it starts at index "startIndex"
    * - it ends   at index "endIndex" - 1
    * - it contains a number of complete rows (no half rows)
    */
    private static int countLines( byte[] array, int startIndex, int endIndex, Stat stat )
    {
        assert (( startIndex >=0 ) &&
                    ( endIndex <= array.length ) &&
                        ( startIndex < endIndex ));

        int linesCounter   = 0;
        int lastStartIndex = 0;

        for ( int index = startIndex; index < endIndex; index++ ) // "index" is incremented manually - Range doesn't fit here
        {
            if ( endOfLine( array[ index ] ))
            {
                int offset = lastStartIndex;
                int length = ( index - lastStartIndex );

                assert (( offset >= 0 ) && ( length > 0 ));
                analyze( new String( array, offset, length, "UTF-8" ), stat );

                linesCounter++;

                while(( index < endIndex ) && endOfLine( array[ index ] )){ index++ } // Skipping "end of line" sequence
                assert ( endOfLine( array[ index - 1 ] ) && (( index == endIndex ) || ( ! endOfLine( array[ index ] ))));

                lastStartIndex = index;
            }
        }

        return linesCounter;
    }



   /**
    * Analyzes the String specified according to benchmark needs
    * See http://wikis.sun.com/display/WideFinder/The+Benchmark
    *     http://groovy.codehaus.org/Regular+Expressions
    *
    */
    private static void analyze ( String line, Stat stat )
    {
        Matcher m = ( line =~ PATTERN );
        assert ( m && m[ 0 ] ), "Line [$line] doesn't match"

// TODO
// http://code.google.com/p/groovypptest/issues/detail?id=30

        String clientAddress = m.group( 1 ); // m[ 0 ][ 1 ];
        String httpMethod    = m.group( 2 ); // m[ 0 ][ 2 ];
        String uri           = m.group( 3 ); // m[ 0 ][ 3 ];
        String statusCode    = m.group( 4 ); // m[ 0 ][ 4 ];
        String byteCount     = m.group( 5 ); // m[ 0 ][ 5 ];
        String referrer      = m.group( 6 ); // m[ 0 ][ 6 ];

// TODO
// http://code.google.com/p/groovypptest/issues/detail?id=25
//        def ( all_ignored, clientAddress, httpMethod, uri, statusCode, byteCount, referrer ) = m[ 0 ];

        assert ( clientAddress && httpMethod && uri && statusCode && byteCount && referrer );

        boolean isArticle = (( httpMethod == 'GET' ) &&
                             ( uri ==~ '^/ongoing/When/\\d{3}x/\\d{4}/\\d{2}/\\d{2}/[^ .]+$' ));
        if ( isArticle )
        {
            stat.addArticle( uri,
                             clientAddress,
                             (( referrer != '-' ) ? referrer : null ));
        }

        stat.addUri( uri,
                     (( byteCount != '-' ) ? Integer.valueOf( byteCount ) : 0 ),
                     ( statusCode == '404' ));
    }


   /**
    * Determines if byte specified is an end-of-line character
    */
    private static boolean endOfLine( byte b )
    {
        return BOOLEANS[ b ];
    }
}