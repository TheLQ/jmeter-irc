**July 28th, 2011: Version 1.1 Available!**

# Introduction #

jmeter-irc is an [IRC](http://en.wikipedia.org/wiki/Internet_Relay_Chat) Plugin for the  [Apache JMeter](http://jakarta.apache.org/jmeter/) load testing program. JMeter provides an existing robust, stable, and extensible framework that just couldn't be matched easily with a custom built load testing program.

jmeter-irc finally gives you the ability to load test IRC bots and clients, with load testing IRC servers coming soon. You can now easily figure out where the bottlenecks are in your client or bot without having to write your own software. You can also discover memory leaks over long periods of testing.

Due to the single digit millisecond response time for some bots, jmeter-irc was written with performance in mind. Current testing on mid to low range PC's show that 9,000 lines per second is easily obtainable, with the upper limit really unknown. While most clients don't need to process that many lines, some network wide bots would benefit from knowing they can processing that many lines. Whatever the needs, jmeter-irc is able to scale to meet them

jmeter-irc rapidly tests with a variety of situations, including
  * Channel message
  * Channel notice
  * Channel action (/ME IRC command)
  * Private message
  * Private action
  * Command in the channel for bots
  * Command in private message for bots
  * Setting and removing operator status
  * Setting and removing voice status
  * Kicking and joining
  * Banning and unbanning
  * Parting and joining
  * Quiting and joining

# Downloading and Usage #

jmeter-irc **1.1** is available for download [here](http://code.google.com/p/jmeter-irc/downloads/detail?name=jmeter-irc-client-1.1.jar). Note that only JMeter 2.4 is supported, future versions may or may not work

jmeter-irc has a tiny built in IRC server thats heavily integrated with the samplers. Multiple clients can connect, but doing so will slow down the testing considerably and can have unpredictable results.

To setup JMeter
  1. Download jmeter-irc and put in JMeter's lib/ext directory
  1. Run JMeter as usual
  1. Right click Test Plan on the left and goto Add > Threads (Users) > Thread Group. Configure the number of users you want to use, the ramp up period (ignore if your unfamiliar), and the loop count. To run for a time period, use the scheduler.
  1. Right click the Thread Group you created on the left and goto Add > Sampler > IRC - Client
  1. Pick a different port number if necessary and start the IRC server by clicking Start.
  1. Configure any necessary bot information. Nothing should be blank
    * **Target Nick** - The most important field. This should be your bots nick as it joins the server
    * Bot Prefix - The prefix that all "users" will have
    * Channel Prefix - The prefix that all channels will have
    * Channels - The number of channels to use
    * Command - The command to send for the command tests
  1. Configure the Possible Actions the bots can do
    * Note: Commands will be sent in {command} {nick} form
  1. Add a listener to view the results. Results Tree is recommended for first time use to find any errors; add it by right clicking your thread group and going to Add > Listener > View Results Tree. Summary Report is recommended for any real test (significantly faster than Results Tree); right click your Thread Group on the left and goto Add > Listener > Summary Report.

Now you need to configure your bot. jmeter-irc will send IRC Protocol compliant lines. To get realistic results you need to preform all processing and sending normally. A line is sent from a user and that user waits for a response. A user will not send multiple lines at once either, its in a strict Send-Wait-Receive-Send-Wait-Receive system

When a new client tries to connect, the server does the following
  * Wait for a line beginning with NICK (max 5 seconds). The word after is used as the nick
  * Send a 004 status line. This should tell most clients that they are connected
  * Assume any further lines are responses to samples EXCEPT
    * If a JOIN command is received, acknowledge with a JOIN successful response

jmeter-irc measures a sample is considered the total amount of time to send a line to the client and receive the appropriate response. To respond, you will need to send a piece of information back. Listed below are all the lines your client must handle and the expected response. Note that the response will not have spaces
  * Channel Command - Given in {command} {response} format, return the response in the channel
  * Private Message Command - Exact same format as above, return the response as a private message to the user that sent it
  * Channel Message - Return the message in the channel
  * Channel Notice - Return the notice in the channel
  * Channel Action (/ME) - Return the action in the channel
  * Private Message - Return the message as a private message to the user who sent it
  * Set Operator - Return the user that got opped in the channel
  * Remove Operator - Return the user that got deopped in the channel
  * Set Voice - Return the user that received voice in the channel
  * Remove Voice - Return the user that got voice removed in the channel
  * Kick - Return the reason the user was kicked
  * Ban - Return the ban nick
  * Unban - Return the ban nick
  * Join - Return the user's nick (used in several tests)
  * Part - Return the part reason
  * Quit - Return the quit reason

**Note:** Not responding correctly to one of these will cause that user to wait forever, eventually preventing anything else from running. You must respond correctly to all lines or uncheck it from the screen

# Limitations #

Due to doing the opposite of JMeter's intended purpose (your the client testing the server vs jmeter-irc where your the server testing the client) and current shortcomings of the code, there are some things that just don't work or are untested

  * Command line only mode - Doesn't work since the server needs to be created, and that is currently only done in the GUI
  * Remote testing - Doesn't work for the same reasons as above
  * Restoring configuration in the GUI from save - Currently this is unsupported, you must reconfigure the bot each time. This will hopefully get fixed in future versions
  * Greater than 999,999,999 users - This crazily large number of users shouldn't ever be reached, but there's a remote chance than long periods of use of JMeter without restarting using various versions of tests could break this limit. Fix: Don't run various tests in the same JMeter instance for weeks, restart every once in a while.
  * Multiple Testing Clients - If you only want to watch the output then you can, but connecting multiple testing clients to the IRC server will break the tests.