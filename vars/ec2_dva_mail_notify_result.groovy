// send run test result in mail
// You can write variables to job_env.yaml
// echo "XXX: YYY" >> ${WORKSPACE}/job_env.yaml
def call(String sender, String receiver) {
    result = readYaml file: "job_env.yaml"
    emailext (
        body: """
${env.JOB_NAME} - ${env.BUILD_DISPLAY_NAME} - ${currentBuild.currentResult}
Check console output at ${env.BUILD_URL}

=================== VirtQE S1 AWS DVA Test Report (TestOwner: ${TESTOWNER} =====================
Summary:
Test method: Automation
Test group:  ${result.TESTSUITE}
Test result: ${result.TESTRESULT}
   - Total: ${result.TOTAL}
   - Pass:  ${result.PASS}
   - Fail:  ${result.FAILURES}
   - Skip: ${result.SKIP}
Note: I will report bugs if there are new failures in the test log.

Push Task:
   - ${result.JOB_TASK_URL}

Debug Log:
   - ${result.HTMLURL}
        """,
        subject: "${env.JOB_NAME} - ${env.BUILD_DISPLAY_NAME} - ${currentBuild.currentResult}",
        from: "${sender}",
        to: "${receiver}"
    )
}