package com.joker.movie;

import com.joker.hunter.Impl;
import com.joker.service.MovieService;

@Impl(service = MovieService.class)
public class MovieServiceImpl implements MovieService {
  @Override public String movieName() {
    return "一出好戏";
  }
}
