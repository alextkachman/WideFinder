package widefinder


/**
 * Statistics class
 */
@Typed
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
    * Retrieves values corresponding to the "top N" counters in the Map specified.
    */
    static Map<String, Long> top ( int n, Map<String, L> map )
    {
        assert (( n > 0 ) && ( map != null ));

        Map<String, Long>             result         = new LinkedHashMap<String, Long>( n );
        Map<Long, Collection<String>> topCountersMap = topCountersMap( n, map );
        topCountersMap.keySet().sort{ long a, long b -> ( b - a ) }.each
        {
            long counter ->

            topCountersMap[ counter ].each
            {
                String s ->

                if ( result.size()  < n ) { result.put( s, counter ) }
                if ( result.size() == n ) return result;
            }
        }

        return result;
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

        Map<Long, Collection<String>> topCountersMap = new HashMap<Long, Collection<String>>( n );
        long[]                        minValue       = [ Long.MAX_VALUE ];

        map.each
        {
            String key, L l ->

            long     counter = l.counter();
            assert ( counter > 0 );

            if (( topCountersMap.size() == n ) && ( counter > minValue ) && ( ! topCountersMap[ counter ] ))
            {
                topCountersMap.remove( minValue );
            }

            if (( topCountersMap.size() < n ) && ( ! topCountersMap[ counter ] ))
            {
                topCountersMap[ counter ] = new ArrayList<String>( n );
                minValue[ 0 ]             = counter;
                topCountersMap.keySet().each{ minValue[ 0 ] = (( it < minValue[ 0 ] ) ? it : minValue[ 0 ] ) }
            }

            if ( topCountersMap.containsKey( counter ) && ( topCountersMap[ counter ].size() < n ))
            {
                topCountersMap[ counter ] << key;
            }
        }

        assert ( topCountersMap.size() <= n );
        return   topCountersMap;
    }
}
