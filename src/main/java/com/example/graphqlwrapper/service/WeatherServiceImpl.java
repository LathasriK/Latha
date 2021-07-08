package com.example.graphqlwrapper.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.example.graphqlwrapper.model.City;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service("weatherService")
public class WeatherServiceImpl implements WeatherService {
  private static final Logger LOG = LoggerFactory.getLogger(WeatherServiceImpl.class);
  private final ObjectMapper mapper = new ObjectMapper();

  private final QueryService queryService;

  @Autowired
  WeatherServiceImpl(QueryService queryService) {
    this.queryService = queryService;
  }

  public ResponseEntity<City> getCityByName(String cityName) throws JsonProcessingException {
    Object result = queryService.getCityByName(cityName);
    String resultAsString = mapper.writeValueAsString(result);
    City city = mapper.readValue(resultAsString, City.class);
    LOG.info("city: {}", city);
    return new ResponseEntity<>(city, HttpStatus.OK);
  }
}
