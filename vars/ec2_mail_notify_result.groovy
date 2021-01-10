// send run test result in mail
// You can write variables to job_env.yaml
// echo "XXX: YYY" >> ${WORKSPACE}/job_env.yaml
def call(Map pipelineParams) {
    result = readYaml file: "job_env.yaml"
    emailext (
        body: """
${env.JOB_NAME} - ${env.BUILD_DISPLAY_NAME} - ${currentBuild.currentResult}
Check console output at ${env.BUILD_URL}

=================== VirtQE S1 AWS Test Report (TestOwner: ${result.TESTOWNER}) =====================
Summary:
Test method: Automation
Test suite:  ${result.TESTSUITE}
Test result: ${result.TESTRESULT}
   - Total: ${result.TOTAL}
   - Pass:  ${result.PASS}
   - Fail:  ${result.FAILURES}
   - Error: ${result.ERRORS}

Auto analyze results:
   - ${result.AUTOCHECK}
Know test failures:
   - ${result.HISTORY}/bugspubview/list/
Note: I will report bugs if there are new failures in the test log.

Main Packages:
   - ${result.COMPOSE_ID}
   - ${result.IMAGE}
Instance Types:
- ${result.JOB_INSTANCE_TYPES}

Debug Log:
   - ${result.HTMLURL}
History Logs:
   - ${result.HISTORY}
        """,
        subject: "${env.JOB_NAME} - ${env.BUILD_DISPLAY_NAME} - ${currentBuild.currentResult}",
        from: "${pipelineParams.MAILSENDER}",
        to: "${pipelineParams.MAILRECEIVER}"
    )
}