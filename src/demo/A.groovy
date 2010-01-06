package demo


@Typed
class A
{

   /**
    * Retrieves values corresponding to the "top N" counters in the Map specified.
    */
    static Map<String, Long> top ( int n, Map<String, Long> map )
    {
        return null;
    }


    public static void main( String ... args )
    {
        top( 10, new B().getSomeMap());
    }

}
