package widefinder

String text    = new File( "e:/Projects/groovy-booster/O.100k.log" ).text;
File   newFile = new File( "e:/Projects/groovy-booster/O.Big.log" );

for( i in 1..100 ){ newFile.append( text ); println i }