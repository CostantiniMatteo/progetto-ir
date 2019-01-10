package cgp.progettoir.webservice;

import cgp.progettoir.engine.QueryEngine;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
public class ApplicationController {

    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public ResponseEntity<List<ResultEntity>> getJobs(@RequestParam("q") String query) {
        var result = QueryEngine.match(query);
        return ResponseEntity.ok(result);
    }

}
