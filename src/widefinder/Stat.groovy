package widefinder


/**
 * Statistics class
 */
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
    static List<String> top ( Map<String, L> map, int n )
    {
        Map<L, Set<String>> valuesMap = new HashMap<L,Set<String>>();

        /**
         * Reverting a map - creating a new one, where counter is mapped to Set<String> of keys
         */
        map.each
        {
            String key, L counter ->

            if ( ! valuesMap[ counter ] )
            {
                valuesMap[ counter ] = new HashSet<String>([ key ]);
            }

            valuesMap[ counter ] << key;
        }

        long[] values = valuesMap.keySet().collect{ it.counter }

        return null;
    }
}
