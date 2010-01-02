package widefinder

import java.nio.ByteBuffer
import java.nio.channels.FileChannel


@Typed
class Start
{
    /**
     * Array of booleans where only "end of line" indices are set to "true"
     */
/*
    private static final boolean[] BOOLEANS = {
                                                    boolean[] b = new boolean[ 256 ];
                                                    b[ ( int ) '\r' ] = true;
                                                    b[ ( int ) '\n' ] = true;
                                                    return  b;
                                              }();
    private static final boolean[] BOOLEANS = ( 0 .. 255 ).collect{ ( it == ( int ) '\r' ) || ( it == ( int ) '\n' ) }
*/
    private static final boolean[] BOOLEANS = getBooleans();
    private static       boolean[]            getBooleans()
    {
        boolean[] b       = new boolean[ 256 ];
        b[ ( int ) '\r' ] = true;
        b[ ( int ) '\n' ] = true;
        return  b;
    };


    public static void main ( String[] args )
    {
        int    bufferSizeMb = 10;
        File   file         = new File( args[ 0 ] );
        assert file.isFile();

        println ( [ "Buffer Size", "CPU #", "Lines #", "Time (sec)" ].join( '\t' ));

        for ( int cpuNum in ( 1 .. 30 ).step( 5 ))
        {
            long            t          = System.currentTimeMillis();
            int             bufferSize = Math.min( file.size(), ( bufferSizeMb * 1024 * 1024 ));
            ByteBuffer      buffer     = ByteBuffer.allocate( bufferSize );
            FileInputStream fis        = new FileInputStream( file );
            FileChannel     channel    = fis.getChannel();

            try
            {
                print ( [ bufferSize, cpuNum, "" ].join( '\t' ));
                long lines = countLines( channel, buffer, cpuNum );
                println ([ lines, (( System.currentTimeMillis() - t ) / 1000 ) ].join( '\t' ));
            }
            finally
            {
                channel.close();
                fis.close();
            }

            buffer = null;
            10.times{ System.gc(); sleep( 1000 ); }
        }
    }


   /**
    * Reads number of lines in the channel specified
    */
    private static long countLines ( FileChannel channel, ByteBuffer buffer, int cpuNum )
    {
        buffer.rewind();

        long totalLines     = 0;
        long totalBytesRead = 0;

        /**
         * Reading from file channel into buffer (until it ends)
         */
        for ( int remaining = 0; ( channel.position() < channel.size()); )
        {
            int    bytesRead = channel.read( buffer );
            totalBytesRead  += bytesRead;
            byte[] array     = buffer.array();
            boolean isEof    = ( channel.position() == channel.size());

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

                totalLines += countLines( array, startIndex, endIndex );
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
    private static int countLines( byte[] array, int startIndex, int endIndex )
    {
        int linesCounter  = 0;

        assert (( startIndex >=0 ) &&
                    ( endIndex <= array.length ) &&
                        ( startIndex < endIndex ));

        for ( int j = startIndex; j < endIndex; j++ )
        {
            if ( endOfLine( array[ j ] ))
            {
                linesCounter++;
                while(( j < endIndex ) && endOfLine( array[ j ] )){ j++ }
            }
        }

        return linesCounter;
    }


   /**
    * Determines if byte specified is an end-of-line character
    */
    private static boolean endOfLine( byte b )
    {
        return BOOLEANS[ b ];
    }
}