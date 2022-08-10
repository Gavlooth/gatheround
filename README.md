Flexiana app                                                                                                                                    
To create the java (backend app) hit lein uberjar (the author assumes what the reader knows what lein uberjar and hit is)                      
Both the java and javascript  apps require a config.edn to be present to the same folder. There is one there for you to                       
change ports or hosts

To compile the javascript files run ``shadow-cljs release app``   but you have to provide your own solution for  hosting the file, you can   
have nginx to server it  or use some kind of  socat  script  e.t.c. e.t.c or                                                 
you can hit ``shadow-cljs watch app``  and let node take care of it                                                        
shadow-cljs will consume config.edn throught  a macro for configuration of the http requests, you can check the ports there    

Finaly you can probably figure what is going on by just reading the code and compiling it inside your head  


Enjoy         
             
