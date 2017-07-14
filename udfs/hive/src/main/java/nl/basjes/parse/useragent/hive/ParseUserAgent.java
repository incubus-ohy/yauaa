/*
 * Yet Another UserAgent Analyzer
 * Copyright (C) 2013-2017 Niels Basjes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.basjes.parse.useragent.hive;

import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDF;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.StringObjectInspector;
import org.apache.hadoop.io.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Hive UDF for parsing the UserAgent string.
 * An example statement
 * would be:
 * <pre>
 *   ADD JAR
 ADD JAR hdfs:///yauaa-hive-2.0-SNAPSHOT-udf.jar;


 USING JAR 'hdfs:/plugins/yauaa-hive-2.0-SNAPSHOT-udf.jar';

 SELECT ParseUserAgent('Mozilla/5.0 (X11\; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Safari/537.36');
 SELECT ParseUserAgent(useragent) from useragents;
 *  SELECT ParseUserAgent(useragent) FROM clickLogs a;
 * </pre>
 *
 *
 */

@Description(
    name = "ParseUserAgent",
    value = "_FUNC_(str) - Parses the UserAgent into all possible pieces.",
    extended = "Example:\n" +
        "  > SELECT ParseUserAgent(useragent) FROM clickLogs a;\n" +
        "  xxxxxx  FIXME xxxx") // FIXME:
public class ParseUserAgent extends GenericUDF {

    private transient StringObjectInspector useragentOI = null;
    private transient UserAgentAnalyzer userAgentAnalyzer = null;
    private transient List<String> fieldNames = null;

    @Override
    public ObjectInspector initialize(ObjectInspector[] args) throws UDFArgumentException {
        // ================================
        // Check the input
        assert (args.length == 1); // This UDF accepts one argument
        // The first argument is a list
        ObjectInspector inputOI = args[0];
        if (!(inputOI instanceof StringObjectInspector)) {
            throw new UDFArgumentException("The argument must be a string");
        }
        useragentOI = (StringObjectInspector) inputOI;

        // ================================
        // Initialize the parser
        // TODO: Add feature to only select limited fields
        if (userAgentAnalyzer == null) {
            userAgentAnalyzer = UserAgentAnalyzer
                .newBuilder()
                .hideMatcherLoadStats()
                .build();
            fieldNames = userAgentAnalyzer.getAllPossibleFieldNamesSorted();
        }
        // ================================
        // Define the output
        // https://stackoverflow.com/questions/26026027/how-to-return-struct-from-hive-udf

        // Define the field names for the struct<> and their types
        List<ObjectInspector> fieldObjectInspectors = new ArrayList<>(fieldNames.size());

        for (String ignored : fieldNames) {
            fieldObjectInspectors.add(PrimitiveObjectInspectorFactory.writableStringObjectInspector);
        }
        return ObjectInspectorFactory.getStandardStructObjectInspector(fieldNames, fieldObjectInspectors);
    }

    @Override
    public Object evaluate(DeferredObject[] args) throws HiveException {
        String userAgentString = useragentOI.getPrimitiveJavaObject(args[0].get());

        if (userAgentString == null) {
            return null;
        }

        UserAgent userAgent = userAgentAnalyzer.parse(userAgentString);
        List<Object> result = new ArrayList<>(fieldNames.size());
        for (String fieldName : fieldNames) {
            String value = userAgent.getValue(fieldName);
            if (value == null) {
                result.add(null);
            } else {
                result.add(new Text(value));
            }
        }
        return result.toArray();
    }

    @Override
    public String getDisplayString(String[] args) {
        return "Parses the UserAgent into all possible pieces.";
    }
}