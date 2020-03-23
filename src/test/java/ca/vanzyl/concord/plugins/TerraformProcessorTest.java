package ca.vanzyl.concord.plugins;

import ca.vanzyl.concord.plugins.terraform.TerraformProcessingResult;
import ca.vanzyl.concord.plugins.terraform.TerraformProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.walmartlabs.concord.plugins.TestSupport;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

//
//  - task: terraformProcessor
//    in:
//      version: 0.12
//      directory: terraform
//      region: us-west-2
//      authentication: credentials | assume-role
//      resources:
//        - type: vpc
//          variables:
//            vpc_name: jvz-vpc
//        - type: eks
//          variables:
//            cluster_name: jvz-cluster
//
public class TerraformProcessorTest extends TestSupport
{

    @Test
    public void validateTerraformProcessorWithYaml() throws Exception {

        String awsAccessKeyId = "xxx";
        String awsSecretAccessKey = "yyy";

        String yaml =
                        "version: 0.12\n" +
                        "provider: aws\n" +
                        "authentication: credentials\n" +
                        "configuration:\n" +
                        "  aws_region: us-east-2\n" +
                        "  aws_access_key: \"" + awsAccessKeyId + "\"\n" +
                        "  aws_secret_key: \"" + awsSecretAccessKey + "\"\n" +
                        "  tags:\n" +
                        "    creator: concord\n" +
                        "    session: jvz-001\n" +
                        "resources:\n" +
                        "  - type: vpc\n" +
                        "    variables:\n" +
                        "      vpc_name: jvz-vpc\n" +
                        "      vpc_cidr: 10.206.0.0/18\n" +
                        "      public_subnet_list:\n" +
                        "        - us-east-2a\n" +
                        "        - us-east-2b\n" +
                        "        - us-east-2c\n" +
                        "      private_subnet_list:\n" +
                        "        - us-east-2a\n" +
                        "        - us-east-2b\n" +
                        "        - us-east-2c\n" +
                        "  - type: eks\n" +
                        "    variables:\n" +
                        "      cluster_name: jvz-cluster\n";

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Map<String, Object> map = mapper.readValue(yaml, Map.class);

        Path source = new File(basedir, "src/test/terraform/00-aws/terraform").toPath();
        Path target = new File(basedir,"target/workdir/terraform").toPath();
        Path workDir = new File(basedir,"target/workdir").toPath();

        TerraformProcessor processor = new TerraformProcessor(source, target, workDir);
        TerraformProcessingResult result = processor.process(map);

        ObjectMapper jsonMapper = new ObjectMapper();
        Map<String,Object> json = jsonMapper.readValue(result.terraformVariablesJson(), Map.class);

        //
        // These are the variables that are fed into Terraform to satisfy the variable declarations for the
        // resources we have gathered.
        //

        /*
        {
          "aws_access_key" : "xxx",
          "aws_region" : "us-east-2",
          "aws_secret_key" : "yyy",
          "cluster_name" : "jvz-cluster",
          "private_subnet_map" : {
            "us-east-2a" : 4,
            "us-east-2b" : 5,
            "us-east-2c" : 6
          },
          "public_subnet_map" : {
            "us-east-2a" : 1,
            "us-east-2b" : 2,
            "us-east-2c" : 3
          },
          "tags" : {
            "creator" : "concord",
            "session" : "jvz-001"
          },
          "vpc_cidr" : "10.206.0.0/18",
          "vpc_name" : "jvz-vpc"
        }
         */

        System.out.println(result.terraformVariablesJson());

        assertEquals("xxx", json.get("aws_access_key"));
        assertEquals("yyy", json.get("aws_secret_key"));
        assertEquals("jvz-cluster", json.get("cluster_name"));
        assertEquals("10.206.0.0/18", json.get("vpc_cidr"));
        assertEquals("jvz-vpc", json.get("vpc_name"));

        // Make sure all policy files are copied into the workdir
        Path policyFile = workDir.resolve("eks-policy-aws-s3.json");
        assertTrue(Files.exists(policyFile));
    }
}
