package widefinder


int  j = 0;
long t = System.currentTimeMillis();
new File( "e:/Projects/groovy-booster/O.100k.log" ).text.eachLine{ j++ }
println "[$j] rows, [${ System.currentTimeMillis() - t }] ms"
