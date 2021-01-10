// send mail notification of job status
// eg. mail_notify_simple('xxxx@redhat.com', 'yyyy@redhat.com')
def call(String sender, String receiver, boolean attach_log=false) {
    emailext (
        body: """
${env.JOB_NAME} - ${env.BUILD_DISPLAY_NAME} - ${currentBuild.currentResult}
Check console output at ${env.BUILD_URL}
        """,
        attachLog: ${attach_log},
        subject: "${env.JOB_NAME} - ${env.BUILD_DISPLAY_NAME} - ${currentBuild.currentResult}",
        from: "${sender}",
        to: "${receiver}"
    )
}