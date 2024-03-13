package com.example.lettucemoddemo.controller;
import org.springframework.web.bind.annotation.RestController;

import com.example.lettucemoddemo.model.Person;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.lettucemod.RedisModulesClient;
import com.redis.lettucemod.api.StatefulRedisModulesConnection;
import com.redis.lettucemod.api.sync.RedisModulesCommands;
import com.redis.lettucemod.json.SetMode;
import com.redis.lettucemod.search.CreateOptions;
import com.redis.lettucemod.search.Field;
import com.redis.lettucemod.search.SearchResults;

import io.lettuce.core.RedisURI;

import java.sql.Timestamp;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
public class Controller {
    
    //create modules client
    RedisModulesClient client = RedisModulesClient.create(RedisURI.create("localhost",6379)); 

    //connect to redis server
    StatefulRedisModulesConnection<String, String> connection = client.connect();

    //Obtain the command API for synchronous execution.
    RedisModulesCommands<String, String> commands = connection.sync();


    @GetMapping("/getById")
    public String getPersonById(@RequestParam String id) {
        String key_p = "People:" + id;
        String person = commands.jsonGet(key_p, "$");
        if (person == null) {
            return "id not found";
        } else {
            return person;
        }
       
    }

    @PostMapping("/new")
    public String addPerson(@RequestBody Person p) {
        String p_json;
        try {
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            String time = timestamp.toString();
            p.setCreatedOn(time);
            p_json = new ObjectMapper().writeValueAsString(p);
            String pid = p.getId();
            String key_p = "People:" + pid ;
            commands.jsonSet(key_p, "$", p_json , SetMode.NX);
            return p_json;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "error";
        }  
    }

    @GetMapping("/getByName")
    public String getPersonByName(@RequestParam String n) {
        try {
            CreateOptions<String, String> options = CreateOptions.<String, String>builder()
                                                     .on(CreateOptions.DataType.JSON)
                                                     .prefix("People:")
                                                     .build();
            commands.ftCreate("pinx", options, Field.text("$.name").build(), Field.numeric("$.age").build());
        } catch (Exception e) {
            System.out.println("already exists index");
        }
        SearchResults<String , String> s = commands.ftSearch("pinx", n);
        System.out.println(s);
        if (s.isEmpty() == true) {
            return "name not found";
        } else {
            return s.toString();
        } 
    }

    @DeleteMapping("/deleteById")
    public String deletePersonById(@RequestParam String id) {
        String key_p = "People:" + id ;
        commands.jsonDel(key_p);
        return "Deleted People : " + id;
    }

    // @PutMapping("/NameById")
    // public String updatePersonNameById(@RequestParam String id , @RequestBody(required = false) String n) {
    //     String key_p = "People:" + id ;
    //     //System.out.println(n);
    //     try {
    //         String updatedPerson = commands.jsonSet(key_p, "$.name", n , SetMode.XX);
    //         System.out.println(updatedPerson);
    //         return "updated";
    //     } catch (Exception e) {
    //         return "error , did not update";
    //     }
        
    // }

    @GetMapping("/getAll")
    public String getAllPerson() {
        SearchResults<String,String> ans = commands.ftSearch("pinx", "*");
        if (ans.isEmpty() == true) {
            return "empty";
        } else {
            return ans.toString();
        } 
    }

    @PutMapping("/updateById")
    public String updatePersonById(@RequestParam String id , @RequestBody(required = false) Map<String,Object> m) {
        String key_p = "People:" + id ;

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String time = timestamp.toString();
        try {
            String t = new ObjectMapper().writeValueAsString(time);
            commands.jsonSet(key_p, "$.updatedOn" ,t , SetMode.XX);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            System.out.println(e);
        }

        Set<String> p_keys = new LinkedHashSet<String>();   
        p_keys.add("name");
        p_keys.add("age");
        p_keys.add("updatedBy");
        Object[] keys = m.keySet().toArray();
        try {
            for (int i = 0 ; i < keys.length ; i ++) {
                String val = new ObjectMapper().writeValueAsString(m.get(keys[i]));
                System.out.println(val);
                if (p_keys.contains(keys[i])) {
                    String path = "$." + keys[i];
                    commands.jsonSet(key_p,path ,val , SetMode.XX);
                }
                else if (keys[i] == "id") {
                        System.out.println("cannot change key");
                }
                else if (keys[i] == "createdBy") {
                    System.out.println("cannot createdBy");
            }
                else {
                        String path = "$.detail." + keys[i];
                        commands.jsonSet(key_p,path ,val);
                }
                }
                return "updated";
            } 
        catch (Exception e) {
            System.out.println(e);
            return "error , did not update";
        }
    }
    
    
    @PutMapping("/updateEntireById")
    public String updatePersonFullById(@RequestParam String id , @RequestBody(required = false) Map<String,Object> m) {
        String key_p = "People:" + id ;
        Object[] keys = m.keySet().toArray();
        String p = commands.jsonGet(key_p, "$");
        try {
            JSONArray array = new JSONArray(p);
            JSONObject object = array.getJSONObject(0); 
            Person person = new ObjectMapper().readValue(object.toString(), Person.class);

            for (int i = 0 ; i < keys.length ; i ++) {
                if (keys[i] == "name") {
                    person.setName(m.get(keys[i]).toString());
                }
                else if (keys[i] == "age") {
                    int a = (Integer) m.get(keys[i]);
                    person.setAge(a);
                }
                else if (keys[i] == "updatedBy") {
                    person.setUpdatedBy(m.get(keys[i]).toString());
                }
                else if (keys[i] == "id") {
                    System.out.println("cannot change key");
                }
                else if (keys[i] == "createdBy") {
                    System.out.println("cannot change createdBy");
                }
                else {
                    person.setDetail(keys[i].toString(), m.get(keys[i]));
                }
            }
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            String time = timestamp.toString();
            person.setUpdatedOn(time);
            String p_json = new ObjectMapper().writeValueAsString(person);
            String updatedPerson = commands.jsonSet(key_p, "$", p_json , SetMode.XX);
            System.out.println(updatedPerson);
            return "updated";
        } catch (Exception e) {
            System.out.println(e);
            return "error , did not update";
        }   
    }


    
    
}
