package demo


@Typed
class A
{

    public static void main( String ... args )
    {
        B b = new B();
        report( 'aaaa', B.top( 10, b.getMap()));
    }


    static void report( String title, Map<String, Long> map )
    {
    }

}
