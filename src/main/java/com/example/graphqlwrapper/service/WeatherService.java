package com.example.graphqlwrapper.service;

import org.springframework.http.ResponseEntity;

import com.example.graphqlwrapper.model.City;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface WeatherService {
  ResponseEntity<City> getCityByName(String cityName) throws JsonProcessingException;
}
