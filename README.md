# GOOFY NATIVE 

Actions speak louder than words : [Youtube link](https://youtube.com/watch?v=XLZkoxvBheI)
Featured in [Cornell Sun](http://cornellsun.com/2016/09/19/students-come-together-to-code-solve-problems-at-the-bigred-hacks/)
Winner of BigRedHacks (Cornell Hackathon) - Best use of Microsoft Services

##Motivation : 
Using multiple smartphones to provide music to a party. Also, choosing songs at the party is always a challenge.

##What it does : 
Goofy solves all the above problems. It takes into account the mood of the user, chooses a song and broadcasts the song to all the devices.

##Build process : 
We created an android application that allows users to click pictures. We used Microsoft’s Cognitive API to detect the mood of the user based on their facial expressions. According to the mood, a song is chosen from Spotify which is then subsequently played on all the phones.

##Challenges : 
We had only read about the time synchronization issues that the distributed systems always face. But this time, we tackled it. We tried streaming the song over TCP but the uncertainties and delays were too huge for the scope of the hackathon. We implemented NTP and modified it to suit our use-case. The server, knowing RTTs for all, helped reach a consensus. The mood detection was fairly simple, thanks to the friendly API by Microsoft Cognitive Services.

##Accomplishments : 
From just a cool-hack to an app that we truly believe can be used for a variety of use-cases, we are happy and proud to reach this point.

##Lessons learnt : 
Distributed Systems. Well, mostly. All jokes apart, we learned a lot of things. Deploying a server on the cloud, interfacing different APIs and the struggle and passion it takes to turn an idea into reality, we learned it all. And then again, using it is so simple and natural. Just as we wanted it to be !

##What's next for Goofy:
We covered parties. But this can be used for a lot different things that we thought of in the process. SSHHH….we are saving them for future hackathons !

##Technology Used:
Node.js, Socket.IO, Android, Microsoft Azure, Microsoft Cognitive API

Screenshot:
<img src="https://github.com/WeekendHacks/goofy-android/blob/master/a9043aaa90413cgoofy-scrnshot.jpg" width=250 height=400>
