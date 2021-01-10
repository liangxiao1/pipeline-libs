// send mail notification of job status
// eg. mail_notify_simple('xxxx@redhat.com', 'yyyy@redhat.com')
def call(Map pipelineParams) {
    emailext (
        body: """
${env.JOB_NAME} - ${env.BUILD_DISPLAY_NAME} - ${currentBuild.currentResult}
Check console output at ${env.BUILD_URL}
        """,
        attachLog: "${pipelineParams.ATTACHLOG}",
        subject: "${env.JOB_NAME} - ${env.BUILD_DISPLAY_NAME} - ${currentBuild.currentResult}",
        from: "${pipelineParams.MAILSENDER}",
        to: "${pipelineParams.MAILRECEIVER}"
    )
}