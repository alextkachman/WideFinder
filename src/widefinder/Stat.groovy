package widefinder


/**
 * Statistics class
 */
@Typed
class Stat
{
    private final Map<String, L>              articlesToHits      = new HashMap<String, L>();
    private final Map<String, L>              uriToByteCounts     = new HashMap<String, L>();
    private final Map<String, L>              uriTo404            = new HashMap<String, L>();
    private final Map<String, Map<String, L>> articlesToClients   = new HashMap<String, Map<String, L>>();
    private final Map<String, Map<String, L>> articlesToReferrers = new HashMap<String, Map<String, L>>();

    Stat ()
    {
    }


    // TODO
    Map<String, L>              getArticlesToHits()      { return this.articlesToHits      }
    Map<String, L>              getUriToByteCounts()     { return this.uriToByteCounts     }
    Map<String, L>              getUriTo404()            { return this.uriTo404            }
    Map<String, Map<String, L>> getArticlesToClients()   { return this.articlesToClients   }
    Map<String, Map<String, L>> getArticlesToReferrers() { return this.articlesToReferrers }



    private static L get( Map<String, L> map, String key )
    {
        assert( key && ( map != null ));

        if ( ! map[ key ] ) { map[ key ] = new L() }
        L      counter = map[ key ];
        assert counter;
        return counter;
    }


    private static L get( Map<String, Map<String, L>> map, String key, String secondKey )
    {
        assert( key && secondKey && ( map != null ));

        if ( ! map[ key ] ) { map[ key ] = new HashMap<String, L>() }

        Map<String, L> secondMap = map[ key ];
        assert       ( secondMap != null );

        return get( secondMap, secondKey );
    }


    L articlesToHitsCounter      ( String articleUri )                       { get( this.articlesToHits,  articleUri  ) }
    L uriToByteCountsCounter     ( String uri        )                       { get( this.uriToByteCounts, uri         ) }
    L uriTo404Counter            ( String uri        )                       { get( this.uriTo404,        uri         ) }
    L clientsToArticlesCounter   ( String articleUri, String clientAddress ) { get( this.articlesToClients,   articleUri, clientAddress ) }
    L referrersToArticlesCounter ( String articleUri, String referrer      ) { get( this.articlesToReferrers, articleUri, referrer      ) }



    /**
     *
     */
    void addArticle( String articleUri, String clientAddress, String referrer )
    {
        assert( articleUri && clientAddress );

        articlesToHitsCounter( articleUri ).increment();
        clientsToArticlesCounter( articleUri, clientAddress ).increment();

        if ( referrer )
        {
            referrersToArticlesCounter( articleUri, referrer ).increment()
        }
    }


    /**
     *
     */
     void addUri( String uri, int bytes, boolean is404 )
    {
        assert( uri );

        if ( bytes > 0 ) { uriToByteCountsCounter( uri ).add( bytes ) }
        if ( is404     ) { uriTo404Counter( uri ).increment()         }
    }



   /**
    *
    */
    static Map<String, Long> top ( int n, Map<String, Long> topArticles, Map<String, Map<String, L>> countersMap )
    {
        assert ( topArticles.size() <= n );

        /**
         * Collection of maps (key => counter) corresponding to top articles
         */
        List<Map<String, L>> maps = new ArrayList<Map<String,L>>( n );

        topArticles.keySet().each
        {
            String topArticle ->

            if ( countersMap[ topArticle ] ) { maps << countersMap[ topArticle ] }
        }

        return top( n, maps.toArray( new Map<String, L>[ maps.size() ] ));
    }


   /**
    * Retrieves values corresponding to the "top N" counters in the Map specified.
    */
    static Map<String, Long> top ( int n, Map<String, L> ... maps )
    {
        assert (( n > 0 ) && ( maps != null ));

        Map<String, Long>             resultMap      = new LinkedHashMap<String, Long>( n );
        Map<Long, Collection<String>> topCountersMap = topCountersMap( n, maps );

       /**
        * Iterating over all counters sorted in decreasing order (from top to bottom)
        * and filling the result map (no more than n entries)
        */
        topCountersMap.keySet().sort{ long a, long b -> ( b - a ) }.each
        {
            long topCounter ->

            /**
             * Iterating over each String corresponding to "top counter"
             */
            topCountersMap[ topCounter ].each
            {
                if ( resultMap.size() < n ) { resultMap.put( it, topCounter ) }
            }
        }

        assert ( resultMap.size() <= n );
        return   resultMap;
    }


   /**
    * Creates a small "top counters" Map (of size n) from a BIG "key => counter" maps:
    *
    * - Key (Long)                 - top n counters found in the map specified
    * - Value (Collection<String>) - original map's keys that were mapped to that key (counter).
    *                                (no more than n)
    *
    * "Top n counter" means that a counter is in "top n" elements if all original counters
    * (values of the map specified) were sorted but we use no sorting here since it's not needed.
    */
    private static Map<Long, Collection<String>> topCountersMap ( int n, Map<String, L> ... maps )
    {
        assert (( n > 0 ) && ( maps != null ));

        Map<Long, Collection<String>> topCountersMap = new HashMap<Long, Collection<String>>( n );
        long[]                        minCounter     = [ Long.MAX_VALUE ]; // Currently known minimal counter

        maps.each
        {
            Map<String, L> map ->

            map.each
            {
                String key, L l ->

                long     counter = l.counter();
                assert ( counter > 0 );

                if (( topCountersMap.size() == n ) && ( counter > minCounter[ 0 ] ) && ( ! topCountersMap[ counter ] ))
                {
                    topCountersMap.remove( minCounter[ 0 ] );
                }

                if (( topCountersMap.size() < n ) && ( ! topCountersMap[ counter ] ))
                {
                    topCountersMap[ counter ] = new ArrayList<String>( n );
                    minCounter[ 0 ]           = counter;
                    topCountersMap.keySet().each{ minCounter[ 0 ] = (( it < minCounter[ 0 ] ) ? it : minCounter[ 0 ] ) }
                }

                if ( topCountersMap.containsKey( counter ) && ( topCountersMap[ counter ].size() < n ))
                {
                    topCountersMap[ counter ] << key;
                }
            }
        }

        assert ( topCountersMap.size() <= n );
        return   topCountersMap;
    }
}
