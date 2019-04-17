package com.amazon.android.tv.tenfoot.ui.sliders;


import com.amazon.android.tv.tenfoot.ui.sliders.Movie;

import java.util.ArrayList;
import java.util.List;

public final class MovieList {
  public static List<Movie> list;

  public static List<Movie> setupMovies() {
    list = new ArrayList<Movie>();

    String bgImageUrl[] = {
        "http://commondatastorage.googleapis.com/android-tv/Sample%20videos/Zeitgeist/Zeitgeist%202010_%20Year%20in%20Review/bg.jpg",
        "http://commondatastorage.googleapis.com/android-tv/Sample%20videos/Demo%20Slam/Google%20Demo%20Slam_%2020ft%20Search/bg.jpg",
        "http://commondatastorage.googleapis.com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Gmail%20Blue/bg.jpg",
        "http://commondatastorage.googleapis.com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Google%20Fiber%20to%20the%20Pole/bg.jpg",
        "http://commondatastorage.googleapis.com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Google%20Nose/bg.jpg",
    };
    String cardImageUrl[] = {
        "http://commondatastorage.googleapis.com/android-tv/Sample%20videos/Zeitgeist/Zeitgeist%202010_%20Year%20in%20Review/card.jpg",
        "http://commondatastorage.googleapis.com/android-tv/Sample%20videos/Demo%20Slam/Google%20Demo%20Slam_%2020ft%20Search/card.jpg",
        "http://commondatastorage.googleapis.com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Gmail%20Blue/card.jpg",
        "http://commondatastorage.googleapis.com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Google%20Fiber%20to%20the%20Pole/card.jpg",
        "http://commondatastorage.googleapis.com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Google%20Nose/card.jpg"
    };

    list.add(buildMovieInfo("category", cardImageUrl[0], bgImageUrl[0]));
    list.add(buildMovieInfo("category", cardImageUrl[1], bgImageUrl[1]));
    list.add(buildMovieInfo("category", cardImageUrl[2], bgImageUrl[2]));
    list.add(buildMovieInfo("category", cardImageUrl[3], bgImageUrl[3]));
    list.add(buildMovieInfo("category", cardImageUrl[4], bgImageUrl[4]));

    return list;
  }

  private static Movie buildMovieInfo(String category, String cardImageUrl,
                                      String bgImageUrl) {
    Movie movie = new Movie();
    movie.setId(Movie.getCount());
    Movie.incCount();
    movie.setCategory(category);
    movie.setCardImageUrl(cardImageUrl);
    movie.setBackgroundImageUrl(bgImageUrl);

    return movie;
  }
}