
package org.schemaspy;


import java.io.IOException;
import java.lang.invoke.MethodHandles;
//import java.sql.Connection;
//import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import org.schemaspy.model.*;
import org.schemaspy.service.*;
import java.sql.DatabaseMetaData;
import org.schemaspy.service.DatabaseService;
import org.schemaspy.service.SqlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class DBFuzzer{

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public  SqlService sqlService;

    private  DatabaseService databaseService;

    private SchemaAnalyzer analyzer;

    public DBFuzzer(SchemaAnalyzer analyzer)
    {
      this.sqlService = Objects.requireNonNull(analyzer.getSqlService());
      this.databaseService = Objects.requireNonNull(analyzer.getDatabaseService());
      this.analyzer = analyzer;
    }





    public boolean fuzz (Config config)
    {
        boolean returnStatus = true;

        LOGGER.info("Starting Database Fuzzing");
        Row randomRow = pickRandomRow();
        Mutation firstMutation = new Mutation(randomRow);
        firstMutation.initPotential_changes(firstMutation.discoverMutationPossibilities(analyzer.getDb()));
        LOGGER.info(firstMutation.getPotential_changes().toString());
        try
        {
          firstMutation.inject(firstMutation.getPotential_changes().get(0),analyzer);
        }
        catch(Exception e)
        {
            LOGGER.error(e.toString());
            returnStatus = false;
        }

      return returnStatus;
    }


    //extract Random row from the db specified in sqlService
    public Row pickRandomRow()
    {

      Table randomTable = pickRandomTable();

      String theQuery = "SELECT * FROM "+randomTable.getName()+" ORDER BY RANDOM() LIMIT 1";
      QueryResponseParser qrp = new QueryResponseParser();
      ResultSet rs = null;
      Row res = null ;
      PreparedStatement stmt;

        try
        {
             stmt = sqlService.prepareStatement(theQuery);
             rs = stmt.executeQuery();
             res = qrp.parse(rs,randomTable).getRows().get(0);
        }
        catch (Exception e)
        {
          LOGGER.info("This query threw an error"+e);
        }

        return res;
    }

    public Table pickRandomTable()
    {
        Random rand = new Random();

        int i = 0, n = rand.nextInt(analyzer.getDb().getTablesMap().entrySet().size());

          for (Map.Entry<String, Table> entry : analyzer.getDb().getTablesMap().entrySet())
          {
            if(n == i)
              return entry.getValue();
            i++;
          }
          throw new RuntimeException("Random table wasn't found"); // should never be reached
    }

}
