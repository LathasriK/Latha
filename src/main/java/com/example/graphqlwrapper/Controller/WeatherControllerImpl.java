package com.example.graphqlwrapper.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.example.graphqlwrapper.model.City;
import com.example.graphqlwrapper.service.WeatherService;
import com.fasterxml.jackson.core.JsonProcessingException;

@Controller
public class WeatherControllerImpl implements WeatherController {
  private static final Logger LOG = LoggerFactory.getLogger(WeatherControllerImpl.class);

  private final WeatherService weatherService;

  @Autowired
  WeatherControllerImpl(WeatherService weatherService) {
    this.weatherService = weatherService;
  }

  public ResponseEntity<City> getCityByName(String cityName) throws JsonProcessingException {
    LOG.info("cityName: {}", cityName);
    return weatherService.getCityByName(cityName);
  }
}
