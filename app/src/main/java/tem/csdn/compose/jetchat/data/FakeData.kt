package tem.csdn.compose.jetchat.data

//import tem.csdn.compose.jetchat.R
//import tem.csdn.compose.jetchat.chat.ChatDataScreenState
//import tem.csdn.compose.jetchat.conversation.ConversationUiState
//import tem.csdn.compose.jetchat.conversation.Message
//import tem.csdn.compose.jetchat.model.User


///**
// * Example colleague profile
// */
//val colleagueProfile = User(
//    "12345",
//    "丁钰",
//    "钰儿",
//    "老色皮",
//    false,
//    null,
//    null,
//    null
//)
//
///**
// * Example "me" profile.
// */
//val meProfile = User(
//    "54321",
//    "Kairlec",
//    "SunfoKairlec",
//    "Senior Android Dev at Yearin\nGoogle Developer Expert",
//    false,
//    "https://github.com/kairlec", "545886742", null
//)
//
//val chatData = ChatDataScreenState(
//    photo = R.drawable.jetchat_logo.toString(),
//    displayName = "SCN105NB"
//)
//
//private val initialMessages = listOf(
//    Message(
//        meProfile,
//        "1",
//        "Check it out!",
//        1620990557
//    ),
//    Message(
//        meProfile,
//        "2",
//        "Thank you!",
//        1620990497,
//        R.drawable.sticker.toString()
//    ),
//    Message(
//        colleagueProfile,
//        "3",
//        "You can use all the same stuff",
//        1620990017
//    ),
//    Message(
//        colleagueProfile,
//        "3",
//        "@54321 Take a look at the `Flow.collectAsState()` APIs",
//        1620988277
//    ),
//    Message(
//        colleagueProfile,
//        "4",
//        "Compose newbie as well, have you looked at the JetNews sample? Most blog posts end up " +
//                "out of date pretty fast but this sample is always up to date and deals with async " +
//                "data loading (it's faked but the same idea applies) \uD83D\uDC49" +
//                "https://github.com/android/compose-samples/tree/master/JetNews",
//        1620963077
//    ),
//    Message(
//        meProfile,
//        "5",
//        "Compose newbie: I’ve scourged the internet for tutorials about async data loading " +
//                "but haven’t found any good ones. What’s the recommended way to load async " +
//                "data and emit composable widgets?",
//        1618975877
//    )
//)
//val exampleUiState = ConversationUiState(
//    initialMessages = initialMessages,
//    channelName = "#composers",
//    channelMembers = 42
//){}