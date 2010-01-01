package widefinder

import java.nio.ByteBuffer
import java.nio.channels.FileChannel


@Typed
class Start
{
//    private static final int     CPU_NUM   = Runtime.getRuntime().availableProcessors();
//    private static final File DATA_FILE = new File( "e:/Projects/groovy-booster/data/O.Big.log" );
    private static final int  R         = (( int ) '\r' );
    private static final int  N         = (( int ) '\n' );


    public static void main ( String[] args )
    {
        File   file = new File( args[ 0 ] );
        assert file.isFile();

        println ( [ "Buffer Size", "CPU #", "Lines #", "Time (sec)" ].join( '\t' ));

        for ( int cpuNum in ( 1 .. 30 ).step( 5 ))
        {
            for ( int bufferSizeMb in ( 10 .. 80 ).step( 10 ))
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

                    assert ( lines = 11000000L );
                }
                finally
                {
                    channel.close();
                    fis.close();
                }

                buffer = null;
                for ( int j in ( 1 .. 10 )){ System.gc(); Thread.sleep( 1000 ); }
            }
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
         * Reading from channel until it ends
         *
         * REMAINING BUFFER
         */
        for ( int remaining = 0; ( channel.position() < channel.size()); )
        {
            int    bytesRead = channel.read( buffer );
            byte[] array     = buffer.array();
            totalBytesRead  += bytesRead;
            boolean eof      = ( channel.position() == channel.size());

            assert (( bytesRead > 0 ) &&
                        (( bytesRead + remaining ) == buffer.position()) &&
                            ( buffer.position()    <= array.length ));

            /**
             * Iterating through buffer, giving each thread it's own String to analyze
             * "beginIndex" - ????????????????
             * "endIndex"   - ????????????????
             */
            int beginIndex = 0;
            int chunkSize  = ( buffer.position() / cpuNum ); // Approximate size of byte[] chunk to be given to each thread
            if ( chunkSize < 1024 ) { chunkSize = buffer.position() }

            // CHUNK SIZE MAY BE TOO SMALLLLLLLLLLLLLLLLLLLLLLLLLLLLL -------------- (remain of file, too many threads)
            for ( int endIndex = chunkSize; ( endIndex <= buffer.position()); endIndex += chunkSize )
            {
                /**
                 * "beginIndex" - ????????????????
                 * "endIndex"   - ????????????????
                 */

                if ((( buffer.position() - endIndex ) < chunkSize ) && ( eof ))
                {
                    /**
                     * Expanding it to the end of current input - otherwise, remaining bytes will be left in buffer
                     * and taken by no thread
                     */
                    endIndex = buffer.position();
                }
                else
                {
                    /**
                     * failed on \r\n sequence - looking where it ends
                     */
                    while (( endIndex < buffer.position()) && endOfLine( array[ endIndex ] )) { endIndex++ }

                    /**
                     * didn't fail on \r\n sequence - looking for it
                     * WHAT IF THERES NO ENOUGH LINES IN BUFFER FOR EACH THREAD?????? ---------------------------------
                     * ???????????????????????????????????
                     */
                    while ( ! endOfLine( array[ endIndex - 1 ] )) { endIndex--; assert ( endIndex > 0 ) }
                }

                assert (( endOfLine( array[ endIndex - 1 ] )) &&
                            (( endIndex == buffer.position()) || ( ! endOfLine( array[ endIndex ] ))));

                totalLines += countLines( array, beginIndex, endIndex );
                beginIndex  = endIndex;
            }

            buffer.position( beginIndex );  // Moving buffer's position a little back - to the last known "endIndex"
            remaining = buffer.remaining(); // Now we know how many bytes are left unread in it
            buffer.compact();               // Copying remaining bytes to the beginning of the buffer
        }

        assert ( totalBytesRead == channel.size()); // Making sure we read all file's data
        return totalLines;
    }


    private static int countLines( byte[] array, int beginIndex, int endIndex )
    {
        int linesCounter  = 0;

        assert (( beginIndex >=0 ) && ( endIndex <= array.length ) && ( beginIndex < endIndex ));

        for ( int j = beginIndex; j < endIndex; j++ )
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
        // NEEEEEEEDDDDDDDDDD Faster mapping ????????????????????
        return (( b == 0x0D ) || ( b == 0x0A ));
    }
}