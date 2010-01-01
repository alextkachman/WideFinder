package widefinder

//@Typed
class B
{

    static void main ( String[] args )
    {
        int  j = 0;
        long t = System.currentTimeMillis();
        new File( "e:/Projects/groovy-booster/O.100k.log" ).eachLine{ j++ }
        println "[$j] rows, [${ System.currentTimeMillis() - t }] ms"
    }
}
