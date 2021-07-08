package com.example.graphqlwrapper.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.graphqlwrapper.model.City;
import com.fasterxml.jackson.core.JsonProcessingException;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(value = "v1")
public interface WeatherController {
  @ApiOperation(value = "", nickname = "getCityByName", response = City.class)
  @GetMapping(value = "/v1/cities/{cityName}", produces = "application/json")
  ResponseEntity<City> getCityByName(@PathVariable String cityName) throws JsonProcessingException;
}
