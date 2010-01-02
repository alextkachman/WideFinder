package widefinder


/**
 * Mutable "long" wrapper
 */
class L
{
    private long counter = 0;

    L ()
    {
    }

    void add ( long l )
    {
        assert ( l > 0 );
        this.counter += l;
    }

    void increment ()
    {
        this.counter++;
    }

    long getCounter ()
    {
        return this.counter;
    }


    def String toString ()
    {
        return String.valueOf( getCounter());
    }
}
