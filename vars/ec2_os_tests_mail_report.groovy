// send run test result in mail
// You can write variables to job_env.yaml
// echo "XXX: YYY" >> ${WORKSPACE}/job_env.yaml
def call(Map pipelineParams) {
    def result = readYaml file: "job_env.yaml"
    emailext (
        body: """
${env.JOB_NAME} - ${env.BUILD_DISPLAY_NAME} - ${currentBuild.currentResult}
Check console output at ${env.BUILD_URL}

=================== VirtQE S1 AWS Test Report (TestOwner: ${TESTOWNER}) =====================
Summary:
Test method: Automation
Test suite:  ${result.TESTSUITE}
Test result:
${result.TESTRESULT}
Note: all failures are known except to_investigate, I will review them.

Main Packages:
   - ${result.COMPOSE_ID}
   - ${result.IMAGE}
Debug Log:
   - ${result.HTMLURL}
History Logs:
   - ${env.LOG_SERVER}/ui/#aws
        """,
        subject: "${env.JOB_NAME} - ${env.BUILD_DISPLAY_NAME} - ${currentBuild.currentResult}",
        from: "${pipelineParams.MAILSENDER}",
        to: "${pipelineParams.MAILRECEIVER}"
    )
}