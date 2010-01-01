long t = System.currentTimeMillis();
int j = 0;
new File( "O.Big.log" ).eachLine{ j++ }
println j;
println "[${ System.currentTimeMillis() - t }] ms"