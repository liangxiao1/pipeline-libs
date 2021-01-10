// install or upgrade dva
def call() {
    sh '''
    #!/bin/bash -x
    echo "Update dva"
    cd ${WORKSPACE}
    cd ..
    sudo yum install -y gcc automake autoconf make nfs-utils
    if ! [[ -e dva_venv/bin/activate ]]; then
        virtualenv -p python2 dva_venv
    fi
    source dva_venv/bin/activate
    pip install -U gevent
    pip install -U wheel
    cd ${WORKSPACE}/dva
    python setup.py bdist_wheel
    sudo pip install -U dist/dva-0.6-py2-none-any.whl
    sudo pip uninstall -y dva
    sudo pip install -U dist/dva-0.6-py2-none-any.whl
    deactivate
    sudo pip install -U dist/dva-0.6-py2-none-any.whl
    sudo pip uninstall -y dva
    sudo pip install -U dist/dva-0.6-py2-none-any.whl
    cd ${WORKSPACE}/aws-sdk-go
    if [[ -e ${JENKINS_HOME}/workspace/dva_venv/lib/python2.7/site-packages/boto/ ]]; then
        cp -f ./models/endpoints/endpoints.json ${JENKINS_HOME}/workspace/dva_venv/lib/python2.7/site-packages/boto/
    elif [[ -e ${JENKINS_HOME}/workspace/dva_venv/lib/python2.7/site-packages/boto-2.49.0-py2.7.egg/boto/ ]]; then
        cp -f ./models/endpoints/endpoints.json ${JENKINS_HOME}/workspace/dva_venv/lib/python2.7/site-packages/boto-2.49.0-py2.7.egg/boto/
    elif [[ -e /usr/lib/python2.7/site-packages/boto ]]; then
        sudo cp -f ./models/endpoints/endpoints.json /usr/lib/python2.7/site-packages/boto
    elif [[ -e /usr/lib/python2.7/site-packages/boto-2.49.0.dist-info/boto/ ]]; then
        sudo cp -f ./models/endpoints/endpoints.json /usr/lib/python2.7/site-packages/boto-2.49.0.dist-info/boto/
    fi
    rm -rf ${WORKSPACE}/aws-sdk-go

    '''
}