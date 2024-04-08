package com.example.lettucemoddemo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.matchers.Any;
//import org.mockito.Mock.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;

import com.example.lettucemoddemo.controller.Controller;
import com.example.lettucemoddemo.model.Person;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.lettucemod.RedisModulesClient;
import com.redis.lettucemod.api.StatefulRedisModulesConnection;
import com.redis.lettucemod.api.sync.RedisModulesCommands;
import com.redis.lettucemod.json.SetMode;
import com.redis.lettucemod.search.Document;
import com.redis.lettucemod.search.SearchOptions;
import com.redis.lettucemod.search.SearchOptions.SortBy;

import jakarta.servlet.http.HttpServletResponse;

import com.redis.lettucemod.search.SearchResults;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class junitTest {

  @Mock
  StatefulRedisModulesConnection<String, String> connection;

  @Mock
  RedisModulesCommands<String, String> commands;

  @InjectMocks
  Controller controller;

  @Test
  public void testApiGetByName() throws Exception {

    Person person1 = new Person();
    person1.setId("1000");
    person1.setName("tk");
    person1.setAge(22);
    person1.setactive0(true);

    Person person2 = new Person();
    person2.setId("1001");
    person2.setName("tk");
    person2.setAge(25);
    person2.setactive0(true);

    String jsonPerson1 = new ObjectMapper().writeValueAsString(person1);
    String jsonPerson2 = new ObjectMapper().writeValueAsString(person2);

    when(connection.sync()).thenReturn(commands);

    // case 1 : name does not exist
    String query1;
    String name1 = "xyz";
    query1 = "(@name:" + name1 + " & @active0:{true})";
    SearchResults<String, String> searchResults1 = new SearchResults<>();
    // set up mock behavior
    when(commands.ftSearch("pidx", query1, controller.options2)).thenReturn(searchResults1);
    // call method using mock object
    ResponseEntity<Map<String, Object>> result1 = controller.getPersonByName("xyz");
    System.out.println(result1);
    // verify that mock behavior was called
    verify(commands).ftSearch("pidx", query1, controller.options2);
    // check result
    assertEquals(400, result1.getBody().get("code"));

    // case 2 : name exist
    String query2;
    String name2 = "tk";
    query2 = "(@name:" + name2 + " & @active0:{true})";
    SearchResults<String, String> searchResults2 = new SearchResults<String, String>();

    // searchResults2 = personlist;
    Document document1 = new Document<>();
    document1.put("id", "1000");
    document1.put("$", jsonPerson1);

    Document document2 = new Document<>();
    document2.put("id", "1001");
    document2.put("$", jsonPerson2);

    searchResults2.add(document1);
    searchResults2.add(document2);

    // System.out.println(searchResults2);

    // set up mock behavior
    when(commands.ftSearch("pidx", query2, controller.options2)).thenReturn(searchResults2);
    // call method using mock object
    ResponseEntity<Map<String, Object>> result2 = controller.getPersonByName("tk");
    System.out.println(result2);

    List<Object> personList = new ArrayList<Object>();
    personList = Arrays.asList(result2.getBody().get("data"));
    System.out.println(personList.get(0).getClass());

    // verify that mock behavior was called
    verify(commands).ftSearch("pidx", query2, controller.options2);
    // check result
    assertEquals("OK", result2.getBody().get("success"));

  }

  @Test
  public void testApiDeleteById() throws Exception {

    Person person1 = new Person();
    person1.setId("1000");
    person1.setName("tk");
    person1.setAge(22);
    person1.setactive0(true);

    Person person2 = new Person();
    person2.setId("8888");
    person2.setName("abc");
    person2.setAge(27);
    person2.setactive0(false);

    List<Person> personList1 = new ArrayList<Person>();
    personList1.add(person1);

    List<Person> personList2 = new ArrayList<Person>();
    personList2.add(person2);

    when(connection.sync()).thenReturn(commands);

    // case 1 : id does not exist
    String hashKey1 = "version:People:" + "9999";
    // set up mock behavior
    when(commands.hget(hashKey1, "v")).thenReturn(null);
    // call method using mock object
    ResponseEntity<Map<String, Object>> result1 = controller.deletePersonById("9999", "user2");
    System.out.println(result1);
    // verify that mock behavior was called
    verify(commands).hget(hashKey1, "v");
    // check result
    assertEquals("enter valid id", result1.getBody().get("message"));

    // case 2 : id already deleted
    String hashKey2 = "version:People:" + "8888";
    String key2 = "People:8888:0";
    String jsonPerson2 = new ObjectMapper().writeValueAsString(personList2);

    when(commands.hget(hashKey2, "v")).thenReturn("20");
    when(commands.jsonGet(key2, "$")).thenReturn(jsonPerson2);
    // call method using mock object
    ResponseEntity<Map<String, Object>> result2 = controller.deletePersonById("8888", "user2");
    System.out.println(result2);
    // verify that mock behavior was called
    verify(commands).hget(hashKey2, "v");
    // check result
    assertEquals("enter valid id", result2.getBody().get("message"));

    // case 3 : working
    String hashKey3 = "version:People:" + "1000";
    String key3 = "People:1000:0";
    String jsonPerson3 = new ObjectMapper().writeValueAsString(personList1);
    // set up mock behavior
    when(commands.hget(hashKey3, "v")).thenReturn("5");
    when(commands.jsonGet(key3, "$")).thenReturn(jsonPerson3);

    // call method using mock object
    ResponseEntity<Map<String, Object>> result3 = controller.deletePersonById("1000", "user2");
    System.out.println(result3);
    // verify that mock behavior was called
    verify(commands).hget(hashKey3, "v");
    // check result
    assertEquals("Deleted People :1000", result3.getBody().get("success"));
  }

  @Test
  public void testApiAddNew() throws Exception {

    Person person1 = new Person();
    person1.setId("1000");
    person1.setName("tk");
    person1.setAge(22);

    Person person3 = new Person();
    person3.setId("3000");
    person3.setName("xyz");
    person3.setAge(27);

    Person person2 = new Person();
    person2.setName("abc");
    person2.setAge(25);

    when(connection.sync()).thenReturn(commands);

    // case 1 : audit field not given
    // call method using mock object
    ResponseEntity<Map<String, Object>> result1 = controller.addPerson(person2, null);
    System.out.println(result1);
    // check result
    assertEquals("ERROR : enter audit field user", result1.getBody().get("message"));

    // case 2 : id not given
    // call method using mock object
    ResponseEntity<Map<String, Object>> result2 = controller.addPerson(person2, "user2");
    System.out.println(result2);
    // check result
    assertEquals("cannot create without id", result2.getBody().get("message"));

    // case 3 : id already exists
    String key1 = "People:1000:1";
    // set up mock behavior
    when(commands.jsonSet(eq(key1), eq("$"), anyString(), eq(SetMode.NX))).thenReturn(null);
    // call method using mock object
    ResponseEntity<Map<String, Object>> result3 = controller.addPerson(person1, "user2");
    System.out.println(result3);
    // verify that mock behavior was called
    verify(commands).jsonSet(eq(key1), eq("$"), anyString(), eq(SetMode.NX));
    // check result
    assertEquals("id already exists", result3.getBody().get("message"));

    // case 4 : working
    String key2 = "People:3000:1";
    person3.setcreatedBy0("user2");
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    String time = timestamp.toString();
    person3.setcreatedOn0(time);
    String jsonPerson1 = new ObjectMapper().writeValueAsString(person3);
    // set up mock behavior
    when(commands.jsonSet(eq(key2), eq("$"), anyString(), eq(SetMode.NX))).thenReturn("OK");
    // call method using mock object
    ResponseEntity<Map<String, Object>> result4 = controller.addPerson(person3, "user2");
    System.out.println(result4);
    // verify that mock behavior was called
    verify(commands).jsonSet(eq(key2), eq("$"), anyString(), eq(SetMode.NX));
    // check result
    assertEquals("created new person " + person3.getId(), result4.getBody().get("message"));

  }

  @Test
  public void testApiUpdateById() throws Exception {

    Map<String, Object> map = new LinkedHashMap<String, Object>();
    Map<String, Object> detail = new LinkedHashMap<String, Object>();

    Person person1 = new Person();
    person1.setId("1000");
    person1.setName("tk");
    person1.setAge(22);
    person1.setDetail("sport", "cricket");
    person1.setactive0(true);

    List<Person> personList1 = new ArrayList<Person>();
    personList1.add(person1);

    Person person2 = new Person();
    person2.setId("9000");
    person2.setName("abc");
    person2.setAge(22);
    person2.setactive0(true);

    Person person3 = new Person();
    person3.setId("5000");
    person3.setName("xyz");
    person3.setAge(22);
    person3.setactive0(false);

    when(connection.sync()).thenReturn(commands);

    // case 1 : id does not exist / already deleted
    String personKey1 = "People:8000:0";
    // set up mock behavior
    when(commands.jsonGet(personKey1 , "$")).thenReturn(null);
    // call method using mock object
    ResponseEntity<Map<String, Object>> result1 =
    controller.updatePersonById("8000", "user2" , null);
    System.out.println(result1);
    // verify that mock behavior was called
    verify(commands).jsonGet(personKey1 , "$");
    // check result
    assertEquals("enter relevant id", result1.getBody().get("message"));

    // case 2 : delta null / empty
    String personKey2 = "People:5000:0";
    String jsonPerson2 = new ObjectMapper().writeValueAsString(personList1);
    // set up mock behavior
    when(commands.jsonGet(personKey2 , "$")).thenReturn(jsonPerson2);
    // call method using mock object
    ResponseEntity<Map<String, Object>> result2 =
    controller.updatePersonById("5000", "user2" , map);
    System.out.println(result2);
    // verify that mock behavior was called
    verify(commands).jsonGet(personKey2 , "$");
    // check result
    assertEquals("enter relevant data to update",
    result2.getBody().get("message"));

    // case 3 : working
    String personKey3 = "People:1000:0";
    String jsonPerson3 = new ObjectMapper().writeValueAsString(personList1);

    when(commands.jsonGet(personKey3, "$")).thenReturn(jsonPerson3);

    detail.put("sport", "volleyball");
    detail.put("pet", "dog");

    map.put("id", "14");
    map.put("detail", detail);
    map.put("name", "alistair");
    map.put("age", 20);
    map.put("ps5", "fifa24");

    String jsonMap = new ObjectMapper().writeValueAsString(map);

    Map<String, Object> delta = new LinkedHashMap<String, Object>();
    delta.put("data", map);

    String user = new ObjectMapper().writeValueAsString("user2");
    when(commands.jsonSet(personKey3, "$.updatedBy0", user, SetMode.XX)).thenReturn("OK");
    delta.put("updatedBy0", user);

    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    String time = timestamp.toString();
    String jsonTime = new ObjectMapper().writeValueAsString(time);
    when(commands.jsonSet(personKey3, "$.updatedOn0", jsonTime, SetMode.XX)).thenReturn("OK");
    delta.put("updatedOn0", time);
    //verify(commands).jsonSet(personKey3, "$.updatedOn0", jsonTime, SetMode.XX);

    String hashKey1 = "version:People:1000" ;
    when(commands.hget(hashKey1, "v")).thenReturn("5");
    when(commands.hset(hashKey1, "v", "6")).thenReturn(true);
    String deltaKey = "People:1000:6";

    String value = new ObjectMapper().writeValueAsString(delta);
    when(commands.jsonSet(deltaKey, "$", value, SetMode.NX)).thenReturn("OK");

    // set up mock behavior
    
    // call method using mock object
    ResponseEntity<Map<String, Object>> result3 = controller.updatePersonById("1000", "user2", map);
    System.out.println(result3);
    // verify that mock behavior was called
    verify(commands).jsonGet(personKey3, "$");
    // check result
    assertEquals("updated", result3.getBody().get("message"));

  }

  @Test
  public void testApiGetAll() throws Exception {
    
    when(connection.sync()).thenReturn(commands);

    // case 1 : get all empty list
    SearchOptions<String, String> options1 = SearchOptions.<String, String>builder()
                .limit(0, 5)
                .returnFields("name")
                .sortBy(SortBy.asc("id"))
                .build();

    
    // set up mock behavior
    when(commands.ftSearch(eq("pidx"), eq("@active0:{true}"), any())).thenReturn(null);
    // call method using mock object
    ResponseEntity<Map<String, Object>> result1 = controller.getAllPerson(null, null, null, null);
    System.out.println(result1);
    // verify that mock behavior was called
    verify(commands).ftSearch(eq("pidx"), eq("@active0:{true}"), any());
    // check result
    assertEquals(400, result1.getBody().get("code"));

    Person person1 = new Person();
    person1.setId("1000");
    person1.setName("tk");
    person1.setAge(22);
    person1.setactive0(true);

    Person person2 = new Person();
    person2.setId("1001");
    person2.setName("abc");
    person2.setAge(25);
    person2.setactive0(true);

    Person person3 = new Person();
    person3.setId("5000");
    person3.setName("xyz");
    person3.setAge(22);
    person3.setactive0(false);

    //case 2 : active only , no limit , no offset
    SearchOptions<String, String> options2 = SearchOptions.<String, String>builder()
                .limit(0, 5)
                .returnFields("name")
                .sortBy(SortBy.asc("id"))
                .build();

    SearchResults<String, String> searchResults1 = new SearchResults<>();

    // searchResults1 = personlist;
    Document document1 = new Document<>();
    document1.put("name", person1.getName());
    document1.setId("People:1000:0");
    

    Document document2 = new Document<>();
    document2.put("name", person2.getName());
    document2.setId("People:1001:0");

    searchResults1.add(document1);
    searchResults1.add(document2);

    // set up mock behavior
    when(commands.ftSearch(eq("pidx"), eq("@active0:{true}"), any())).thenReturn(searchResults1);
    System.out.println(searchResults1);
     MockHttpServletResponse response = new MockHttpServletResponse();
    // call method using mock object
    ResponseEntity<Map<String, Object>> result2 = controller.getAllPerson(null, null, null, response);
    System.out.println(result2);
    // verify that mock behavior was called
    verify(commands , times(2)).ftSearch(eq("pidx"), eq("@active0:{true}"), any());
    // check result
    assertEquals(200, result2.getBody().get("code"));


    //inact true 
    SearchResults<String, String> searchResults2 = new SearchResults<>();

    Document document3 = new Document<>();
    document3.put("name", person3.getName());
    document3.setId("People:5000:0");

    searchResults2.add(document1);
    searchResults2.add(document2);
    searchResults2.add(document3);


    SearchOptions<String, String> options3 = SearchOptions.<String, String>builder()
                .limit(0, 5)
                .returnFields("name")
                .sortBy(SortBy.asc("id"))
                .build();

    // set up mock behavior
    when(commands.ftSearch(eq("pidx"), eq("*"), any())).thenReturn(searchResults2);
    System.out.println(searchResults2);
    // call method using mock object
    ResponseEntity<Map<String, Object>> result3 = controller.getAllPerson(true, null, null, response);
    System.out.println(result3);
    // verify that mock behavior was called
    verify(commands).ftSearch(eq("pidx"), eq("*"), any());
    // check result
    assertEquals(200, result3.getBody().get("code"));

    //limit = 1, offset = 1  
    SearchResults<String, String> searchResults3 = new SearchResults<>();

    Document document4 = new Document<>();
    document4.put("name", person3.getName());
    document4.setId("People:5000:0");

    searchResults3.add(document2);
    searchResults3.setCount(3);


    SearchOptions<String, String> options4 = SearchOptions.<String, String>builder()
                .limit(1, 1)
                .returnFields("name")
                .sortBy(SortBy.asc("id"))
                .build();

    // set up mock behavior
    when(commands.ftSearch(eq("pidx"), eq("*"), any())).thenReturn(searchResults3);
    System.out.println(searchResults3);
    // call method using mock object
    ResponseEntity<Map<String, Object>> result4 = controller.getAllPerson(true, 1, 1, response);
    System.out.println(result4);
    // verify that mock behavior was called
    verify(commands , times(2)).ftSearch(eq("pidx"), eq("*"), any());
    // check result
    assertEquals(200, result4.getBody().get("code"));

  }


  @Test
  public void testApiGetById() throws Exception {

    Person person1 = new Person();
    person1.setId("1000");
    person1.setName("tk");
    person1.setAge(22);
    person1.setDetail("sport", "cricket");
    person1.setactive0(true);

    Map<String, Object> map1 = new LinkedHashMap<String, Object>();
    Map<String, Object> delta1 = new LinkedHashMap<String, Object>();
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    String time = timestamp.toString();
  
    map1.put("name", "tanishka");
    delta1.put("data", map1);
    delta1.put("updatedBy0", "user2");
    delta1.put("updatedOn0", time);


    Map<String, Object> map2 = new LinkedHashMap<String, Object>();
    Map<String, Object> detail2 = new LinkedHashMap<String, Object>();
    Map<String, Object> delta2 = new LinkedHashMap<String, Object>();
    Timestamp timestamp2 = new Timestamp(System.currentTimeMillis());
    String time2 = timestamp.toString();
  
    detail2.put("sport", "football");
    map2.put("age", 45);
    map2.put("detail", detail2);
    delta2.put("data", map2);
    delta2.put("updatedBy0", "user3");
    delta2.put("updatedOn0", time2);

    Person person0 = person1;
    person0.setName("tanishka");
    person0.setAge(45);
    person0.setDetail("sport", "football");
    person0.setupdatedBy0("user3");
    person0.setupdatedOn0(time2);


    List<Map<String, Object>> deltaList1 = new ArrayList<Map<String, Object>>();
    deltaList1.add(delta1);
    String jsonDelta1 = new ObjectMapper().writeValueAsString(deltaList1);

    List<Map<String, Object>> deltaList2 = new ArrayList<Map<String, Object>>();
    deltaList2.add(delta2);
    String jsonDelta2 = new ObjectMapper().writeValueAsString(deltaList2);

    List<Person> personList1 = new ArrayList<Person>();
    personList1.add(person1);
    String jsonPerson1 = new ObjectMapper().writeValueAsString(personList1);

    List<Person> personList0 = new ArrayList<Person>();
    personList0.add(person0);
    String jsonPerson0 = new ObjectMapper().writeValueAsString(personList0);

    Person person2 = new Person();
    person2.setId("5000");
    person2.setName("xyz");
    person2.setAge(22);
    person2.setactive0(false);

    List<Person> personList3 = new ArrayList<Person>();
    personList3.add(person2);
    String jsonPerson3 = new ObjectMapper().writeValueAsString(personList3);


    when(connection.sync()).thenReturn(commands);

    //case 1: id does not exist
    String personKey1 = "People:1:0";
    // set up mock behavior
    when(commands.jsonGet(personKey1, "$")).thenReturn(null);
    // call method using mock object
    ResponseEntity<Map<String, Object>> result1 = controller.getPersonById("1", null, null);
    System.out.println(result1);
    // verify that mock behavior was called
    verify(commands).jsonGet(personKey1, "$");
    // check result
    assertEquals("enter valid id", result1.getBody().get("message"));


    //case 2: min null max null
    String personKey2 = "People:1000:0";
    // set up mock behavior
    when(commands.jsonGet(personKey2, "$")).thenReturn(jsonPerson0);
    // call method using mock object
    ResponseEntity<Map<String, Object>> result2 = controller.getPersonById("1000", null, null);
    System.out.println(result2);
    // verify that mock behavior was called
    verify(commands).jsonGet(personKey2, "$");
    // check result
    assertEquals(200, result2.getBody().get("code"));



    //case 3: min null max 2
    String personKey3 = "People:1000:0";
    String personKey4 = "People:1000:1";
    String deltaKey1 = "People:1000:2";

    // set up mock behavior
    when(commands.jsonGet(personKey3, "$")).thenReturn(jsonPerson0);
    when(commands.jsonGet(personKey4, "$")).thenReturn(jsonPerson1);
    when(commands.jsonGet(deltaKey1, "$")).thenReturn(jsonDelta1);

    // delta_v = commands.jsonGet(key_v, "$");
    // call method using mock object
    ResponseEntity<Map<String, Object>> result3 = controller.getPersonById("1000", null, 2);
    System.out.println(result3);
    // verify that mock behavior was called
    verify(commands, times(2)).jsonGet(personKey3, "$");
    // check result
    assertEquals(200, result3.getBody().get("code"));


    //case 4: min 1 AND max null
    String personKey5 = "People:1000:0";
    String personKey6 = "People:1000:1";
    String deltaKey2 = "People:1000:2";
    String deltaKey3 = "People:1000:3";
    String hashKey2 = "version:People:1000";
    // set up mock behavior
    when(commands.hget(hashKey2, "v")).thenReturn("3");
    when(commands.jsonGet(personKey5, "$")).thenReturn(jsonPerson0);
    when(commands.jsonGet(personKey6, "$")).thenReturn(jsonPerson1);
    when(commands.jsonGet(deltaKey2, "$")).thenReturn(jsonDelta1);
    when(commands.jsonGet(deltaKey3, "$")).thenReturn(jsonDelta2);

    // call method using mock object
    ResponseEntity<Map<String, Object>> result4 = controller.getPersonById("1000", 1, null);
    System.out.println(result4);
    // verify that mock behavior was called
    verify(commands, times(3)).jsonGet(personKey5, "$");
    // check result
    assertEquals(200, result4.getBody().get("code"));


    //case 5: min 2 AND max 3
    String personKey7 = "People:1000:0";
    String personKey8 = "People:1000:1";
    String deltaMaxKey3 = "People:1000:3";
    String deltaMinKey = "People:1000:2";
    // set up mock behavior
    when(commands.jsonGet(personKey7, "$")).thenReturn(jsonPerson0);
    when(commands.jsonGet(personKey8, "$")).thenReturn(jsonPerson1);
    when(commands.jsonGet(deltaMinKey, "$")).thenReturn(jsonDelta1);
    when(commands.jsonGet(deltaMaxKey3, "$")).thenReturn(jsonDelta2);

    // call method using mock object
    ResponseEntity<Map<String, Object>> result5 = controller.getPersonById("1000", 2, 3);
    System.out.println(result5.getBody().get("base"));
    Object testOutput = result5.getBody().get("base");

    ObjectMapper mapper = new ObjectMapper();
    Person checkPerson = mapper.convertValue(testOutput, Person.class);
    System.out.println(checkPerson.getName());
    

    // verify that mock behavior was called
    verify(commands , times(4)).jsonGet(personKey5, "$");
    // check result
    assertEquals("tanishka", checkPerson.getName());


    //case 1: id deleted (inactive)
    String personKey9 = "People:5000:0";
    // set up mock behavior
    when(commands.jsonGet(personKey9, "$")).thenReturn(jsonPerson3);
    // call method using mock object
    ResponseEntity<Map<String, Object>> result6 = controller.getPersonById("5000", null, null);
    System.out.println(result6);
    // verify that mock behavior was called
    verify(commands).jsonGet(personKey9, "$");
    // check result
    assertEquals("enter valid id", result1.getBody().get("message"));


  }



}
