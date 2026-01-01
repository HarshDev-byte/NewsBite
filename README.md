# NewsBite 📰

A modern Android news reader app built with Kotlin, featuring AI-powered article summarization, infinite scroll pagination, offline caching, and Material Design 3.

![Android](https://img.shields.io/badge/Android-26%2B-green?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1-purple?logo=kotlin)
![License](https://img.shields.io/badge/License-MIT-blue)

## ✨ Features

| Feature | Description |
|---------|-------------|
| 🗞️ **Category Browsing** | Browse news by General, Business, Sports, Technology, Health, Entertainment, Science |
| � **Seearch** | Search articles across all categories |
| 🤖 **AI Summarization** | Get AI-powered bullet-point summaries using Google Gemini |
| 🔖 **Bookmarks** | Save articles for later reading |
| 🌙 **Dark Mode** | System-aware theme with manual toggle |
| 📴 **Offline Support** | Read cached news without internet |
| ♾️ **Infinite Scroll** | Seamless pagination with Paging 3 + RemoteMediator |
| 🔄 **Pull-to-Refresh** | Swipe down to refresh news feed |
| ✨ **Shimmer Loading** | Beautiful loading animations |
| 📤 **Share** | Share articles with friends |

## 📱 Screenshots

<!-- Add your screenshots here -->
<!-- ![Home](screenshots/home.png) ![Dark Mode](screenshots/dark.png) ![AI Summary](screenshots/summary.png) -->

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                              │
│  Activities • Adapters • ViewBinding • Material Design 3     │
├─────────────────────────────────────────────────────────────┤
│                    ViewModel Layer                           │
│  NewsViewModel • StateFlow • LiveData • Paging Integration   │
├─────────────────────────────────────────────────────────────┤
│                   Repository Layer                           │
│  NewsRepository • BookmarkRepository • Single Source of Truth│
├─────────────────────────────────────────────────────────────┤
│                     Data Layer                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │  Retrofit    │  │    Room      │  │   Gemini     │       │
│  │  NewsAPI     │  │   Database   │  │     AI       │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
└─────────────────────────────────────────────────────────────┘
```

**Pattern:** MVVM with Repository Pattern  
**DI:** Hilt for dependency injection  
**Pagination:** Paging 3 with RemoteMediator for offline-first caching

## 🛠️ Tech Stack

| Category | Technology |
|----------|------------|
| Language | Kotlin 2.1 |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 (Android 15) |
| Architecture | MVVM |
| DI | Hilt |
| Networking | Retrofit + OkHttp |
| Database | Room |
| Pagination | Paging 3 + RemoteMediator |
| Async | Kotlin Coroutines + Flow |
| AI | Google Generative AI (Gemini) |
| Image Loading | Picasso |
| UI | Material Design 3, ViewBinding |
| Testing | Kotest, MockK |

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog or later
- JDK 17
- Android SDK 35

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/newsbite.git
   cd newsbite
   ```

2. **Get API Keys**
   - News API: [https://newsapi.org/](https://newsapi.org/) (Free tier available)
   - Gemini API (optional): [https://aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey)

3. **Configure API Keys**
   
   Copy the example file and add your keys:
   ```bash
   cp local.properties.example local.properties
   ```
   
   Edit `local.properties`:
   ```properties
   NEWS_API_KEY=your_news_api_key_here
   GEMINI_API_KEY=your_gemini_api_key_here  # Optional
   ```

4. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   ```
   Or open in Android Studio and click Run.

## 📦 Building Release APK

1. **Create a keystore**
   ```bash
   keytool -genkey -v -keystore newsbite.jks -keyalias newsbite -keyalg RSA -keysize 2048 -validity 10000
   ```

2. **Add signing config to `local.properties`**
   ```properties
   KEYSTORE_FILE=path/to/newsbite.jks
   KEYSTORE_PASSWORD=your_password
   KEY_ALIAS=newsbite
   KEY_PASSWORD=your_key_password
   ```

3. **Uncomment signing config in `app/build.gradle.kts`**

4. **Build release**
   ```bash
   ./gradlew assembleRelease
   ```

## 📁 Project Structure

```
app/src/main/java/com/example/newsbite/
├── data/
│   ├── ai/              # Gemini AI summarization service
│   ├── api/             # Retrofit API service
│   ├── local/           # Room database, DAOs, entities
│   ├── model/           # Data models
│   ├── paging/          # Paging 3 RemoteMediator
│   └── repository/      # Repository implementations
├── di/                  # Hilt modules (Network, Database)
├── ui/                  # ViewModels
├── util/                # Utilities (Resource, ThemeManager)
├── MainActivity.kt      # Main news feed
├── NewsFullActivity.kt  # Article WebView + AI summary
├── BookmarksActivity.kt # Saved articles
├── SplashActivity.kt    # Splash screen
└── *Adapter.kt          # RecyclerView adapters
```

## 🧪 Testing

Run unit tests:
```bash
./gradlew test
```

The project includes property-based tests using Kotest for:
- NewsRepository
- NewsRemoteMediator

## 🔑 API Notes

- **NewsAPI Free Tier Limitations:**
  - 100 requests/day
  - Headlines only (no full article content)
  - Results limited to 100 articles per query

- **Gemini AI:**
  - Optional feature - app works without it
  - Summarizes articles based on title and description

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [NewsAPI](https://newsapi.org/) for the news data
- [Google Gemini](https://ai.google.dev/) for AI summarization
- [Facebook Shimmer](https://github.com/facebook/shimmer-android) for loading animations

---

Made with ❤️ using Kotlin
