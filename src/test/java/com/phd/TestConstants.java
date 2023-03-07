package com.phd;

public class TestConstants {
    public static final String  classData =  "diff --git a/plugins/cloud/aws/src/main/java/org/elasticsearch/cloud/aws/AwsEc2Service.java b/plugins/cloud/aws/src/main/java/org/elasticsearch/cloud/aws/AwsEc2Service.java\n" +
            "index fd7e3e718f52d..989ecc64b6197 100644\n" +
            "--- a/plugins/cloud/aws/src/main/java/org/elasticsearch/cloud/aws/AwsEc2Service.java\n" +
            "+++ b/plugins/cloud/aws/src/main/java/org/elasticsearch/cloud/aws/AwsEc2Service.java\n" +
            "@@ -68,6 +68,18 @@ public synchronized AmazonEC2 client() {\n" +
            "             throw new ElasticSearchIllegalArgumentException(\"No s3 secret_key defined for s3 gateway\");\n" +
            "         }\n" +
            " \n" +
            "+        String proxyHost = settings.get(\"network.proxyHost\");\n" +
            "+        if (proxyHost != null) {\n" +
            "+            String portString = settings.get(\"network.proxyPort\",\"80\");\n" +
            "+            Integer proxyPort;\n" +
            "+            try {\n" +
            "+                proxyPort = Integer.parseInt(portString,10);\n" +
            "+            } catch (NumberFormatException ex) {\n" +
            "+                throw new ElasticSearchIllegalArgumentException(\"The configured proxy port value [\" + portString + \"] is invalid\",ex);\n" +
            "+            }\n" +
            "+            clientConfiguration.withProxyHost(proxyHost).setProxyPort(proxyPort);\n" +
            "+        }\n" +
            "+\n" +
            "         this.client = new AmazonEC2Client(new BasicAWSCredentials(account, key), clientConfiguration);\n" +
            " \n" +
            "         if (componentSettings.get(\"ec2.endpoint\") != null) {\n" +
            "diff --git a/plugins/cloud/aws/src/main/java/org/elasticsearch/cloud/aws/AwsS3Service.java b/plugins/cloud/aws/src/main/java/org/elasticsearch/cloud/aws/AwsS3Service.java\n" +
            "index 2796456d59705..28539e1be7ddf 100644\n" +
            "--- a/plugins/cloud/aws/src/main/java/org/elasticsearch/cloud/aws/AwsS3Service.java\n" +
            "+++ b/plugins/cloud/aws/src/main/java/org/elasticsearch/cloud/aws/AwsS3Service.java\n" +
            "@@ -68,6 +68,18 @@ public synchronized AmazonS3 client() {\n" +
            "             throw new ElasticSearchIllegalArgumentException(\"No s3 secret_key defined for s3 gateway\");\n" +
            "         }\n" +
            " \n" +
            "+        String proxyHost = settings.get(\"network.proxyHost\");\n" +
            "+        if (proxyHost != null) {\n" +
            "+            String portString = settings.get(\"network.proxyPort\",\"80\");\n" +
            "+            Integer proxyPort;\n" +
            "+            try {\n" +
            "+                proxyPort = Integer.parseInt(portString,10);\n" +
            "+            } catch (NumberFormatException ex) {\n" +
            "+                throw new ElasticSearchIllegalArgumentException(\"The configured proxy port value [\" + portString + \"] is invalid\",ex);\n" +
            "+            }\n" +
            "+            clientConfiguration.withProxyHost(proxyHost).setProxyPort(proxyPort);\n" +
            "+        }\n" +
            "+\n" +
            "         this.client = new AmazonS3Client(new BasicAWSCredentials(account, key), clientConfiguration);\n" +
            " \n" +
            "         if (componentSettings.get(\"s3.endpoint\") != null) {\n";

    public static final String packageData = "diff --git a/modules/elasticsearch/src/main/java/org/elasticsearch/common/unit/DistanceUnit.java b/modules/elasticsearch/src/main/java/org/elasticsearch/common/unit/DistanceUnit.java\n" +
            "index 9304b6ffee9f1..b7e015908debc 100644\n" +
            "--- a/modules/elasticsearch/src/main/java/org/elasticsearch/common/unit/DistanceUnit.java\n" +
            "+++ b/modules/elasticsearch/src/main/java/org/elasticsearch/common/unit/DistanceUnit.java\n" +
            "@@ -35,7 +35,7 @@ public enum DistanceUnit {\n" +
            "         }@Override public double toMiles(double distance) {\n" +
            "             return distance;\n" +
            "         }@Override public double toKilometers(double distance) {\n" +
            "-            return distance / MILES_KILOMETRES_RATIO;\n" +
            "+            return distance * MILES_KILOMETRES_RATIO;\n" +
            "         }\n" +
            "         @Override public String toString(double distance) {\n" +
            "             return distance + \"mi\";\n" +
            "@@ -44,7 +44,7 @@ public enum DistanceUnit {\n" +
            "         @Override public String toString() {\n" +
            "             return \"km\";\n" +
            "         }@Override public double toMiles(double distance) {\n" +
            "-            return distance * MILES_KILOMETRES_RATIO;\n" +
            "+            return distance / MILES_KILOMETRES_RATIO;\n" +
            "         }@Override public double toKilometers(double distance) {\n" +
            "             return distance;\n" +
            "         }\n" +
            "diff --git a/modules/elasticsearch/src/main/java/org/elasticsearch/index/query/xcontent/GeoDistanceFilterParser.java b/modules/elasticsearch/src/main/java/org/elasticsearch/index/query/xcontent/GeoDistanceFilterParser.java\n" +
            "index a43a55bd274f3..fc9922cfcd29c 100644\n" +
            "--- a/modules/elasticsearch/src/main/java/org/elasticsearch/index/query/xcontent/GeoDistanceFilterParser.java\n" +
            "+++ b/modules/elasticsearch/src/main/java/org/elasticsearch/index/query/xcontent/GeoDistanceFilterParser.java\n" +
            "@@ -74,7 +74,8 @@ public class GeoDistanceFilterParser extends AbstractIndexComponent implements X\n" +
            "         double lon = 0;\n" +
            "         String fieldName = null;\n" +
            "         double distance = 0;\n" +
            "-        DistanceUnit unit = null;\n" +
            "+        Object vDistance = null;\n" +
            "+        DistanceUnit unit = DistanceUnit.KILOMETERS; // default unit\n" +
            "         GeoDistance geoDistance = GeoDistance.ARC;\n" +
            "         while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {\n" +
            "             if (token == XContentParser.Token.FIELD_NAME) {\n" +
            "@@ -110,9 +111,9 @@ public class GeoDistanceFilterParser extends AbstractIndexComponent implements X\n" +
            "             } else if (token.isValue()) {\n" +
            "                 if (currentFieldName.equals(\"distance\")) {\n" +
            "                     if (token == XContentParser.Token.VALUE_STRING) {\n" +
            "-                        distance = DistanceUnit.parse(parser.text(), DistanceUnit.KILOMETERS, DistanceUnit.MILES);\n" +
            "+                        vDistance = parser.text(); // a String\n" +
            "                     } else {\n" +
            "-                        distance = parser.doubleValue();\n" +
            "+                        vDistance = parser.numberValue(); // a Number\n" +
            "                     }\n" +
            "                 } else if (currentFieldName.equals(\"unit\")) {\n" +
            "                     unit = DistanceUnit.fromString(parser.text());\n" +
            "@@ -150,8 +151,10 @@ public class GeoDistanceFilterParser extends AbstractIndexComponent implements X\n" +
            "             }\n" +
            "         }\n" +
            " \n" +
            "-        if (unit != null) {\n" +
            "-            distance = unit.toMiles(distance);\n" +
            "+        if (vDistance instanceof Number) {\n" +
            "+            distance = unit.toMiles(((Number)vDistance).doubleValue());\n" +
            "+        } else {\n" +
            "+            distance = DistanceUnit.parse((String)vDistance, unit, DistanceUnit.MILES);\n" +
            "         }\n" +
            " \n" +
            "         MapperService mapperService = parseContext.mapperService();\n" +
            "diff --git a/modules/elasticsearch/src/test/java/org/elasticsearch/index/query/xcontent/SimpleIndexQueryParserTests.java b/modules/elasticsearch/src/test/java/org/elasticsearch/index/query/xcontent/SimpleIndexQueryParserTests.java\n" +
            "index ac4591ad79f96..0691a3a2433b9 100644\n" +
            "--- a/modules/elasticsearch/src/test/java/org/elasticsearch/index/query/xcontent/SimpleIndexQueryParserTests.java\n" +
            "+++ b/modules/elasticsearch/src/test/java/org/elasticsearch/index/query/xcontent/SimpleIndexQueryParserTests.java\n" +
            "@@ -1220,6 +1220,110 @@ private XContentIndexQueryParser queryParser() throws IOException {\n" +
            "         assertThat(filter.distance(), closeTo(12, 0.00001));\n" +
            "     }\n" +
            " \n" +
            "+    @Test public void testGeoDistanceFilter5() throws IOException {\n" +
            "+        IndexQueryParser queryParser = queryParser();\n" +
            "+        String query = copyToStringFromClasspath(\"/org/elasticsearch/index/query/xcontent/geo_distance5.json\");\n" +
            "+        Query parsedQuery = queryParser.parse(query).query();\n" +
            "+        assertThat(parsedQuery, instanceOf(FilteredQuery.class));\n" +
            "+        FilteredQuery filteredQuery = (FilteredQuery) parsedQuery;\n" +
            "+        GeoDistanceFilter filter = (GeoDistanceFilter) filteredQuery.getFilter();\n" +
            "+        assertThat(filter.fieldName(), equalTo(\"location\"));\n" +
            "+        assertThat(filter.lat(), closeTo(40, 0.00001));\n" +
            "+        assertThat(filter.lon(), closeTo(-70, 0.00001));\n" +
            "+        assertThat(filter.distance(), closeTo(12, 0.00001));\n" +
            "+    }\n" +
            "+\n" +
            "+    @Test public void testGeoDistanceFilter6() throws IOException {\n" +
            "+        IndexQueryParser queryParser = queryParser();\n" +
            "+        String query = copyToStringFromClasspath(\"/org/elasticsearch/index/query/xcontent/geo_distance6.json\");\n" +
            "+        Query parsedQuery = queryParser.parse(query).query();\n" +
            "+        assertThat(parsedQuery, instanceOf(FilteredQuery.class));\n" +
            "+        FilteredQuery filteredQuery = (FilteredQuery) parsedQuery;\n" +
            "+        GeoDistanceFilter filter = (GeoDistanceFilter) filteredQuery.getFilter();\n" +
            "+        assertThat(filter.fieldName(), equalTo(\"location\"));\n" +
            "+        assertThat(filter.lat(), closeTo(40, 0.00001));\n" +
            "+        assertThat(filter.lon(), closeTo(-70, 0.00001));\n" +
            "+        assertThat(filter.distance(), closeTo(12, 0.00001));\n" +
            "+    }\n" +
            "+\n" +
            "+    @Test public void testGeoDistanceFilter7() throws IOException {\n" +
            "+        IndexQueryParser queryParser = queryParser();\n" +
            "+        String query = copyToStringFromClasspath(\"/org/elasticsearch/index/query/xcontent/geo_distance7.json\");\n" +
            "+        Query parsedQuery = queryParser.parse(query).query();\n" +
            "+        assertThat(parsedQuery, instanceOf(FilteredQuery.class));\n" +
            "+        FilteredQuery filteredQuery = (FilteredQuery) parsedQuery;\n" +
            "+        GeoDistanceFilter filter = (GeoDistanceFilter) filteredQuery.getFilter();\n" +
            "+        assertThat(filter.fieldName(), equalTo(\"location\"));\n" +
            "+        assertThat(filter.lat(), closeTo(40, 0.00001));\n" +
            "+        assertThat(filter.lon(), closeTo(-70, 0.00001));\n" +
            "+        assertThat(filter.distance(), closeTo(12, 0.00001));\n" +
            "+    }\n" +
            "+\n" +
            "+    @Test public void testGeoDistanceFilter8() throws IOException {\n" +
            "+        IndexQueryParser queryParser = queryParser();\n" +
            "+        String query = copyToStringFromClasspath(\"/org/elasticsearch/index/query/xcontent/geo_distance8.json\");\n" +
            "+        Query parsedQuery = queryParser.parse(query).query();\n" +
            "+        assertThat(parsedQuery, instanceOf(FilteredQuery.class));\n" +
            "+        FilteredQuery filteredQuery = (FilteredQuery) parsedQuery;\n" +
            "+        GeoDistanceFilter filter = (GeoDistanceFilter) filteredQuery.getFilter();\n" +
            "+        assertThat(filter.fieldName(), equalTo(\"location\"));\n" +
            "+        assertThat(filter.lat(), closeTo(40, 0.00001));\n" +
            "+        assertThat(filter.lon(), closeTo(-70, 0.00001));\n" +
            "+        assertThat(filter.distance(), closeTo(12, 0.00001));\n" +
            "+    }\n" +
            "+\n" +
            "+    @Test public void testGeoDistanceFilter9() throws IOException {\n" +
            "+        IndexQueryParser queryParser = queryParser();\n" +
            "+        String query = copyToStringFromClasspath(\"/org/elasticsearch/index/query/xcontent/geo_distance9.json\");\n" +
            "+        Query parsedQuery = queryParser.parse(query).query();\n" +
            "+        assertThat(parsedQuery, instanceOf(FilteredQuery.class));\n" +
            "+        FilteredQuery filteredQuery = (FilteredQuery) parsedQuery;\n" +
            "+        GeoDistanceFilter filter = (GeoDistanceFilter) filteredQuery.getFilter();\n" +
            "+        assertThat(filter.fieldName(), equalTo(\"location\"));\n" +
            "+        assertThat(filter.lat(), closeTo(40, 0.00001));\n" +
            "+        assertThat(filter.lon(), closeTo(-70, 0.00001));\n" +
            "+        assertThat(filter.distance(), closeTo(12, 0.00001));\n" +
            "+    }\n" +
            "+\n" +
            "+    @Test public void testGeoDistanceFilter10() throws IOException {\n" +
            "+        IndexQueryParser queryParser = queryParser();\n" +
            "+        String query = copyToStringFromClasspath(\"/org/elasticsearch/index/query/xcontent/geo_distance10.json\");\n" +
            "+        Query parsedQuery = queryParser.parse(query).query();\n" +
            "+        assertThat(parsedQuery, instanceOf(FilteredQuery.class));\n" +
            "+        FilteredQuery filteredQuery = (FilteredQuery) parsedQuery;\n" +
            "+        GeoDistanceFilter filter = (GeoDistanceFilter) filteredQuery.getFilter();\n" +
            "+        assertThat(filter.fieldName(), equalTo(\"location\"));\n" +
            "+        assertThat(filter.lat(), closeTo(40, 0.00001));\n" +
            "+        assertThat(filter.lon(), closeTo(-70, 0.00001));\n" +
            "+        assertThat(filter.distance(), closeTo(12, 0.00001));\n" +
            "+    }\n" +
            "+\n" +
            "+    @Test public void testGeoDistanceFilter11() throws IOException {\n" +
            "+        IndexQueryParser queryParser = queryParser();\n" +
            "+        String query = copyToStringFromClasspath(\"/org/elasticsearch/index/query/xcontent/geo_distance11.json\");\n" +
            "+        Query parsedQuery = queryParser.parse(query).query();\n" +
            "+        assertThat(parsedQuery, instanceOf(FilteredQuery.class));\n" +
            "+        FilteredQuery filteredQuery = (FilteredQuery) parsedQuery;\n" +
            "+        GeoDistanceFilter filter = (GeoDistanceFilter) filteredQuery.getFilter();\n" +
            "+        assertThat(filter.fieldName(), equalTo(\"location\"));\n" +
            "+        assertThat(filter.lat(), closeTo(40, 0.00001));\n" +
            "+        assertThat(filter.lon(), closeTo(-70, 0.00001));\n" +
            "+        assertThat(filter.distance(), closeTo(12, 0.00001));\n" +
            "+    }\n" +
            "+\n" +
            "+    @Test public void testGeoDistanceFilter12() throws IOException {\n" +
            "+        IndexQueryParser queryParser = queryParser();\n" +
            "+        String query = copyToStringFromClasspath(\"/org/elasticsearch/index/query/xcontent/geo_distance12.json\");\n" +
            "+        Query parsedQuery = queryParser.parse(query).query();\n" +
            "+        assertThat(parsedQuery, instanceOf(FilteredQuery.class));\n" +
            "+        FilteredQuery filteredQuery = (FilteredQuery) parsedQuery;\n" +
            "+        GeoDistanceFilter filter = (GeoDistanceFilter) filteredQuery.getFilter();\n" +
            "+        assertThat(filter.fieldName(), equalTo(\"location\"));\n" +
            "+        assertThat(filter.lat(), closeTo(40, 0.00001));\n" +
            "+        assertThat(filter.lon(), closeTo(-70, 0.00001));\n" +
            "+        assertThat(filter.distance(), closeTo(12, 0.00001));\n" +
            "+    }\n" +
            "+\n" +
            "     @Test public void testGeoBoundingBoxFilterNamed() throws IOException {\n" +
            "         IndexQueryParser queryParser = queryParser();\n" +
            "         String query = copyToStringFromClasspath(\"/org/elasticsearch/index/query/xcontent/geo_boundingbox-named.json\");\n" +
            "diff --git a/modules/elasticsearch/src/test/java/org/elasticsearch/index/query/xcontent/geo_distance10.json b/modules/elasticsearch/src/test/java/org/elasticsearch/index/query/xcontent/geo_distance10.json\n" +
            "new file mode 100644\n" +
            "index 0000000000000..6d08ae7e63800\n" +
            "--- /dev/null\n" +
            "+++ b/modules/elasticsearch/src/test/java/org/elasticsearch/index/query/xcontent/geo_distance10.json\n" +
            "@@ -0,0 +1,17 @@\n" +
            "+{\n" +
            "+    \"filtered\" : {\n" +
            "+        \"query\" : {\n" +
            "+            \"match_all\" : {}\n" +
            "+        },\n" +
            "+        \"filter\" : {\n" +
            "+            \"geo_distance\" : {\n" +
            "+                \"distance\" : 19.312128,\n" +
            "+                \"unit\": \"km\",\n" +
            "+                \"person.location\" : {\n" +
            "+                    \"lat\" : 40,\n" +
            "+                    \"lon\" : -70\n" +
            "+                }\n" +
            "+            }\n" +
            "+        }\n" +
            "+    }\n" +
            "+}\n" +
            "diff --git a/modules/elasticsearch/src/test/java/org/elasticsearch/index/query/xcontent/geo_distance11.json b/modules/elasticsearch/src/test/java/org/elasticsearch/index/query/xcontent/geo_distance11.json\n" +
            "new file mode 100644\n" +
            "index 0000000000000..77ebf781bc2c4\n" +
            "--- /dev/null\n" +
            "+++ b/modules/elasticsearch/src/test/java/org/elasticsearch/index/query/xcontent/geo_distance11.json\n" +
            "@@ -0,0 +1,16 @@\n" +
            "+{\n" +
            "+    \"filtered\" : {\n" +
            "+        \"query\" : {\n" +
            "+            \"match_all\" : {}\n" +
            "+        },\n" +
            "+        \"filter\" : {\n" +
            "+            \"geo_distance\" : {\n" +
            "+                \"distance\" : \"19.312128km\",\n" +
            "+                \"person.location\" : {\n" +
            "+                    \"lat\" : 40,\n" +
            "+                    \"lon\" : -70\n" +
            "+                }\n" +
            "+            }\n" +
            "+        }\n" +
            "+    }\n" +
            "+}\n" +
            "diff --git a/modules/elasticsearch/src/test/java/org/elasticsearch/index/query/xcontent/geo_distance12.json b/modules/elasticsearch/src/test/java/org/elasticsearch/index/query/xcontent/geo_distance12.json\n" +
            "new file mode 100644\n" +
            "index 0000000000000..909c4b537b7c1\n" +
            "--- /dev/null\n" +
            "+++ b/modules/elasticsearch/src/test/java/org/elasticsearch/index/query/xcontent/geo_distance12.json\n" +
            "@@ -0,0 +1,17 @@\n" +
            "+{\n" +
            "+    \"filtered\" : {\n" +
            "+        \"query\" : {\n" +
            "+            \"match_all\" : {}\n" +
            "+        },\n" +
            "+        \"filter\" : {\n" +
            "+            \"geo_distance\" : {\n" +
            "+                \"distance\" : \"12mi\",\n" +
            "+                \"unit\": \"km\",\n" +
            "+                \"person.location\" : {\n" +
            "+                    \"lat\" : 40,\n" +
            "+                    \"lon\" : -70\n" +
            "+                }\n" +
            "+            }\n" +
            "+        }\n" +
            "+    }\n" +
            "+}\n" +
            "diff --git a/modules/elasticsearch/src/test/java/org/elasticsearch/index/query/xcontent/geo_distance5.json b/modules/elasticsearch/src/test/java/org/elasticsearch/index/query/xcontent/geo_distance5.json\n" +
            "new file mode 100644\n" +
            "index 0000000000000..d9c80f3ac35c5\n" +
            "--- /dev/null\n" +
            "+++ b/modules/elasticsearch/src/test/java/org/elasticsearch/index/query/xcontent/geo_distance5.json\n" +
            "@@ -0,0 +1,17 @@\n" +
            "+{\n" +
            "+    \"filtered\" : {\n" +
            "+        \"query\" : {\n" +
            "+            \"match_all\" : {}\n" +
            "+        },\n" +
            "+        \"filter\" : {\n" +
            "+            \"geo_distance\" : {\n" +
            "+                \"distance\" : 12,\n" +
            "+                \"unit\": \"mi\",\n" +
            "+                \"person.location\" : {\n" +
            "+                    \"lat\" : 40,\n" +
            "+                    \"lon\" : -70\n" +
            "+                }\n" +
            "+            }\n" +
            "+        }\n" +
            "+    }\n" +
            "+}\n" +
            "diff --git a/modules/elasticsearch/src/test/java/org/elasticsearch/index/query/xcontent/geo_distance6.json b/modules/elasticsearch/src/test/java/org/elasticsearch/index/query/xcontent/geo_distance6.json\n" +
            "new file mode 100644\n" +
            "index 0000000000000..ebf12b358f848\n" +
            "--- /dev/null\n" +
            "+++ b/modules/elasticsearch/src/test/java/org/elasticsearch/index/query/xcontent/geo_distance6.json\n" +
            "@@ -0,0 +1,17 @@\n" +
            "+{\n" +
            "+    \"filtered\" : {\n" +
            "+        \"query\" : {\n" +
            "+            \"match_all\" : {}\n" +
            "+        },\n" +
            "+        \"filter\" : {\n" +
            "+            \"geo_distance\" : {\n" +
            "+                \"distance\" : \"12\",\n" +
            "+                \"unit\": \"mi\",\n" +
            "+                \"person.location\" : {\n" +
            "+                    \"lat\" : 40,\n" +
            "+                    \"lon\" : -70\n" +
            "+                }\n" +
            "+            }\n" +
            "+        }\n" +
            "+    }\n" +
            "+}\n" +
            "diff --git a/modules/elasticsearch/src/test/java/org/elasticsearch/index/query/xcontent/geo_distance7.json b/modules/elasticsearch/src/test/java/org/elasticsearch/index/query/xcontent/geo_distance7.json\n" +
            "new file mode 100644\n" +
            "index 0000000000000..7a659b15d9ba7\n" +
            "--- /dev/null\n" +
            "+++ b/modules/elasticsearch/src/test/java/org/elasticsearch/index/query/xcontent/geo_distance7.json\n" +
            "@@ -0,0 +1,16 @@\n" +
            "+{\n" +
            "+    \"filtered\" : {\n" +
            "+        \"query\" : {\n" +
            "+            \"match_all\" : {}\n" +
            "+        },\n" +
            "+        \"filter\" : {\n" +
            "+            \"geo_distance\" : {\n" +
            "+                \"distance\" : \"19.312128\",\n" +
            "+                \"person.location\" : {\n" +
            "+                    \"lat\" : 40,\n" +
            "+                    \"lon\" : -70\n" +
            "+                }\n" +
            "+            }\n" +
            "+        }\n" +
            "+    }\n" +
            "+}\n" +
            "diff --git a/modules/elasticsearch/src/test/java/org/elasticsearch/index/query/xcontent/geo_distance8.json b/modules/elasticsearch/src/test/java/org/elasticsearch/index/query/xcontent/geo_distance8.json\n" +
            "new file mode 100644\n" +
            "index 0000000000000..dd9334aae8d8b\n" +
            "--- /dev/null\n" +
            "+++ b/modules/elasticsearch/src/test/java/org/elasticsearch/index/query/xcontent/geo_distance8.json\n" +
            "@@ -0,0 +1,16 @@\n" +
            "+{\n" +
            "+    \"filtered\" : {\n" +
            "+        \"query\" : {\n" +
            "+            \"match_all\" : {}\n" +
            "+        },\n" +
            "+        \"filter\" : {\n" +
            "+            \"geo_distance\" : {\n" +
            "+                \"distance\" : 19.312128,\n" +
            "+                \"person.location\" : {\n" +
            "+                    \"lat\" : 40,\n" +
            "+                    \"lon\" : -70\n" +
            "+                }\n" +
            "+            }\n" +
            "+        }\n" +
            "+    }\n" +
            "+}\n" +
            "diff --git a/modules/elasticsearch/src/test/java/org/elasticsearch/index/query/xcontent/geo_distance9.json b/modules/elasticsearch/src/test/java/org/elasticsearch/index/query/xcontent/geo_distance9.json\n" +
            "new file mode 100644\n" +
            "index 0000000000000..2e62052516a3e\n" +
            "--- /dev/null\n" +
            "+++ b/modules/elasticsearch/src/test/java/org/elasticsearch/index/query/xcontent/geo_distance9.json\n" +
            "@@ -0,0 +1,17 @@\n" +
            "+{\n" +
            "+    \"filtered\" : {\n" +
            "+        \"query\" : {\n" +
            "+            \"match_all\" : {}\n" +
            "+        },\n" +
            "+        \"filter\" : {\n" +
            "+            \"geo_distance\" : {\n" +
            "+                \"distance\" : \"19.312128\",\n" +
            "+                \"unit\": \"km\",\n" +
            "+                \"person.location\" : {\n" +
            "+                    \"lat\" : 40,\n" +
            "+                    \"lon\" : -70\n" +
            "+                }\n" +
            "+            }\n" +
            "+        }\n" +
            "+    }\n" +
            "+}\n";
}
