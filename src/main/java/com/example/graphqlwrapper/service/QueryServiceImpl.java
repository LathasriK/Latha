package com.example.graphqlwrapper.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import graphql.query;

@Service("queryService")
public class QueryServiceImpl implements QueryService {
  private static final Logger LOG = LoggerFactory.getLogger(QueryServiceImpl.class);

  private static final String GRAPHQL_ENDPOINT = "https://graphql-weather-api.herokuapp.com/";

  public Object getCityByName(String cityName) {
    query.GetCityByName builder = query.GetCityByName.builder(cityName).build();
    query.GetCityByName.Result result = builder.request(GRAPHQL_ENDPOINT).post();
    LOG.info("result.getBindings().toJson(): {}", result.getBindings().toJson());
    return result.getBindings().get("getCityByName");
  }
}
