package widefinder


/**
 * Statistics class
 */
//@Typed
class Stat
{
    final Map<String, L>              articlesToHits      = new HashMap<String, L>();
    final Map<String, L>              uriToByteCounts     = new HashMap<String, L>();
    final Map<String, L>              uriTo404            = new HashMap<String, L>();
    final Map<String, Map<String, L>> articlesToClients   = new HashMap<String, Map<String, L>>();
    final Map<String, Map<String, L>> articlesToReferrers = new HashMap<String, Map<String, L>>();

    Stat ()
    {
    }


    private static L get( String key, Map<String, L> map )
    {
        assert( key && ( map != null ));

        if ( ! map[ key ] ) { map[ key ] = new L() }
        L      counter = map[ key ];
        assert counter;
        return counter;
    }


    private static L get( String key, Map<String, Map<String, L>> map, String secondKey )
    {
        assert( key && secondKey && ( map != null ));

        if ( ! map[ key ] ) { map[ key ] = new HashMap<String, L>() }

        Map<String, L> secondMap = map[ key ];
        assert       ( secondMap != null );

        return get( secondKey, secondMap );
    }


    L getArticlesToHits      ( String articleUri )                       { get( articleUri, this.articlesToHits  ) }
    L getUriToByteCounts     ( String uri        )                       { get( uri,        this.uriToByteCounts ) }
    L getUriTo404            ( String uri        )                       { get( uri,        this.uriTo404        ) }
    L getArticlesToClients   ( String articleUri, String clientAddress ) { get( articleUri, this.articlesToClients,   clientAddress ) }
    L getArticlesToReferrers ( String articleUri, String referrer      ) { get( articleUri, this.articlesToReferrers, referrer      ) }



    /**
     *
     */
    void addArticle( String articleUri, String clientAddress, String referrer )
    {
        assert( articleUri && clientAddress );

        getArticlesToHits( articleUri ).increment();
        getArticlesToClients( articleUri, clientAddress ).increment();

        if ( referrer )
        {
            getArticlesToReferrers( articleUri, referrer ).increment()
        }
    }


    /**
     *
     */
     void addUri( String uri, int bytes, boolean is404 )
    {
        assert( uri );

        if ( bytes > 0 ) { getUriToByteCounts( uri ).add( bytes ) }
        if ( is404     ) { getUriTo404( uri ).increment()         }
    }



   /**
    *
    */
    static List<String> top ( int n, Map<String, L> map )
    {
        assert (( n > 0 ) && ( map != null ));

        Map<Long, Collection<String>> topCountersMap = topCountersMap( n, map );
        return null;
    }




   /**
    * Creates a "top counters Map":
    * - Key (Long)                 - top N counter found in the map specified.
    * - Value (Collection<String>) - original map's keys that were mapped to that key (counter).
    *                                No more than N Strings are kept in Collection:
    *                                if there are more - they're discarded.
    *
    * "Top N counter" means that a counter is in "top N" elements if all original counters
    * (values of the map specified) were sorted but we <b>use no sorting here</b> as it's not needed.
    */
    private static Map<Long, Collection<String>> topCountersMap ( int n, Map<String, L> map )
    {
        assert (( n > 0 ) && ( map != null ));

        final long[]                        topCounters     = new long[ n ];
        final Map<Long, Collection<String>> topCountersMap  = new HashMap<Long, Collection<String>>( n );
        int                                 topCountersSize = 0;
        int                                 minIndex        = -1; // index of the minimal element in "topCounters" array
        int                                 minValue        = Integer.MAX_VALUE;

        map.each
        {
            String key, L l ->

            long counter = l.counter();

            if ( topCountersSize < n )
            {
                /**
                 * Initializing "topCounters" array with first N elements
                 */

                if ( topCounters.find{ it == counter } )
                {
                    /**
                     * "topCounters" array already contains this "counter" - we update "topCountersMap"
                     * entry value (Collection<String>) with new String
                     * (only if it contains less than N elements - we don't need more)
                     */
                    if ( topCountersMap[ counter ].size() < n )
                    {
                        topCountersMap[ counter ] << key;
                    }
                }
                else
                {
                    /**
                     * "topCounters" array doesn't contain this counter yet - we add it to array,
                     * create new "topCountersMap" entry (initialized with current "key" String)
                     * and update "minIndex"
                     */
                    topCounters[ topCountersSize++ ] = counter;
                    topCountersMap[ counter ]        = newCollection( n, key );

                    if ( counter < minValue )
                    {
                        minIndex = ( topCountersSize - 1 ); // "topCountersSize" was already incremented so we take 1 back
                        minValue = counter;
                    }
                }
            }
            else
            {

                /**
                 * "topCounters" array is already initialized with first N elements
                 * "minIndex" is also initialized
                 */

                assert (( topCountersSize == n ) && ( minIndex >= 0 ) && ( minIndex < topCounters.size()));

                if (( counter > topCounters[ minIndex ] ) && ( ! topCounters.find { it == counter }))
                {
                    /**
                     * Current "counter" is larger than minimal counter in "topCounters" array and it (array)
                     * doesn't contain this counter yet.
                     *
                     * So we:
                     * - Replace "topCountersMap" entry (corresponding to smaller counter) with a new one
                     *   (corresponding to the current, bigger "counter"),
                     * - Update "topCounters" array - replace previous minimal element at index "minIndex" with "counter"
                     * - Calculate new "minIndex" for the minimal "topCounters" array element
                     */

                    topCountersMap.remove( topCounters[ minIndex ] );
                    topCountersMap[ counter ] = newCollection( n, key );
                    topCounters[ minIndex ]   = counter;
                    minIndex                  = chooseMinIndex( topCounters );
                    int j = 5;
                }
                else if ( topCountersMap[ counter ] && ( topCountersMap[ counter ].size() < n ))
                {
                    /**
                     * "topCountersMap" contains an entry mapping "counter" to Collection<String>
                     * so we add a new entry to it
                     * (only if it contains less than N elements - we don't need more)
                     */

                    topCountersMap[ counter ] << key;
                }
            }
        }

        /**
         * Verifying each "topCountersMap" key is found in "topCounters" array (once!)
         * and each "topCounters" array element has a mapping in "topCountersMap"
         */
        assert (( topCountersSize == n ) && ( topCounters.size() == n ) && ( topCountersMap.size() == n ));
        assert topCountersMap.keySet().every { topCounters.count( it ) == 1 }
        assert topCounters.every{ topCountersMap[ it ] }

        return topCountersMap;
    }


   /**
    *
    */
    static int chooseMinIndex( long[] array )
    {
        assert ( array.size() > 0 );

        int minIndex = 0;
        int minValue = array[ 0 ];

        for ( j in ( 1 ..< array.size()))
        {
            if ( array[ j ] < minValue )
            {
                minIndex = j;
                minValue = array[ j ];
            }
        }

        return minIndex;
    }



   /**
    *
    */
    static Collection<String> newCollection( int size, String firstElement )
    {
        Collection<String> c = new ArrayList<String>( size );
        c << firstElement;
        return c;
    }
}
