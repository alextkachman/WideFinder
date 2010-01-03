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
    *
    */
    static List<String> top ( int n, Map<String, L> map )
    {
        assert (( n > 0 ) && ( map != null ));

        Map<Long, Set<String>> valuesMap   = revertMap( map );
        long[]                 topCounters = top( n, valuesMap.keySet().toArray( new long[ valuesMap.keySet().size() ] ));

        return null;
    }


   /**
    *
    */
    static long[] top ( int n, long[] values )
    {
        assert (( n > 0 ) && ( values != null ));

        if ( values.size() <= n )
        {
            return values;
        }

        assert ( values.size() > n );
        long[] topValues = values[ 0 ..< n ];
        int    minIndex  = minIndex( topValues );

        for ( index in ( n ..< values.size()))
        {
            if ( values[ index ] > topValues[ minIndex ] )
            {
                topValues[ minIndex ] = values[ index ];
                minIndex = minIndex( topValues );
            }
        }

        // TODO
        List<Long> sortedList = topValues.toList().sort{ long a, long b -> ( b - a ) };
        long[]     result     = sortedList.toArray( new long[ topValues.size() ] );
        return     result;
    }



   /**
    *
    */
    static int minIndex( long[] array )
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
    * Reverts a [String => Counter] map to the [Counter => Set<String>] one
    */
    private static Map<Long, Set<String>> revertMap ( Map<String, L> map )
    {
        Map<Long, Set<String>> newMap = new HashMap<Long, Set<String>>();

        map.each
        {
            String key, L counter ->

            if ( ! newMap[ counter.counter() ] ) { newMap[ counter.counter() ] = new LinkedHashSet<String>([ key ]) }
            newMap[ counter.counter() ] << key; // Adding new element to Set<String>
        }

        return newMap;
    }
}
