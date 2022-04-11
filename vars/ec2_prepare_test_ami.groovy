// Build test AMIs from given COMPOSE_URL, the base AMIs are the 7.6,7.7,7.8,8.0,8.1
// Rememeber to keep them up to date.
// Below env's are required
// KEY_NAME=''(keypair name on aws)
// KEYFILE=''(ssh keyfile realpath)
// Below params are required
// params.EC2_REGION=''
// params.EC2_PROFILE=""
// params.EC2_SUBNET=""
// params.EC2_SG_GROUP=""
// params.PROXY_URL=""
// params.ARCH='x86_64|aarch64'

def call() {
    sh '''
    #!/bin/bash -x
    if [[ -e $WORKSPACE/job_env.txt ]]; then
        source $WORKSPACE/job_env.txt
    fi

    if [[ -z $JOB_BASE_AMI ]]; then
        echo "No JOB_BASE_AMI specified, auto select one!"
        source /home/ec2/ec2_venv/bin/activate
        cd /home/ec2/mini_utils/
        if [ -z $JOB_INFO_BUILD_ID ]; then
            python ec2_ami_select.py -f data/branch_map.yaml -c $COMPOSE_ID -s ami_id -d -p ${ARCH}
            ami_id=$(python ec2_ami_select.py -f data/branch_map.yaml -c $COMPOSE_ID -s ami_id -p ${ARCH})
            branch_name=$(python ec2_ami_select.py -f data/branch_map.yaml -c $COMPOSE_ID -s branch_name)
        else
            # sleep 1200 to wait brew tag
            sleep 1200
            python ec2_ami_select.py -f data/branch_map.yaml -c "${JOB_PKGURL}${JOB_BREWTAG//' '}" -s ami_id -d -p ${ARCH}
            ami_id=$(python ec2_ami_select.py -f data/branch_map.yaml -c "${JOB_PKGURL}${JOB_BREWTAG//' '}" -s ami_id -p ${ARCH})
            branch_name=$(python ec2_ami_select.py -f data/branch_map.yaml -c "${JOB_PKGURL}${JOB_BREWTAG//' '}" -s branch_name)
        fi
        if ! [ -z $branch_name ]; then
            echo "BRANCH_NAME=${branch_name}" >> $WORKSPACE/job_env.txt
        fi
        deactivate
    else
        echo "Use specified $BASE_AMI"
        ami_id=$JOB_BASE_AMI
    fi

    ssh_user='ec2-user'

    if [[ $COMPOSE_ID =~ 'RHEL-7' ]]; then
        pkgs="install,automake,autoconf,sysstat,gcc,unzip,wget,quota,bzip2,iperf3,pciutils,fio,psmisc,expect,ntpdate,perf,nvme-cli,pciutils,fio,git,tar,nfs-utils,libvirt,qemu-kvm,kernel-debug,python3,dracut-fips,podman,strace,sos,strace,acpid"
    elif [[ $COMPOSE_ID =~ 'RHEL-8' ]]; then
        pkgs="make,automake,autoconf,sysstat,gcc,unzip,wget,quota,bzip2,iperf3,pciutils,fio,psmisc,expect,perf,nvme-cli,pciutils,fio,php-cli,php-xml,php-json,libaio-devel,blktrace,fio,nvme-cli,git,tar,rng-tools,nfs-utils,libvirt,qemu-kvm,kernel-debug,python3,dracut-fips,podman,xdp-tools,openssl-devel,sos,strace,acpid"
    elif [[ $COMPOSE_ID =~ 'RHEL-9' ]]; then
        pkgs="make,automake,autoconf,sysstat,gcc,unzip,wget,quota,bzip2,iperf3,pciutils,fio,psmisc,expect,perf,nvme-cli,pciutils,fio,libaio-devel,blktrace,fio,nvme-cli,git,tar,rng-tools,nfs-utils,libvirt,qemu-kvm,python3,dracut-fips,kernel-debug,python3-pip,hostname,podman,xdp-tools,openssl-devel,glibc-all-langpacks,sos,strace,acpid"
    elif [[ $COMPOSE_ID =~ 'CentOS-Stream' ]]; then
        #ssh_user='centos'
        pkgs="make,automake,autoconf,sysstat,gcc,unzip,wget,quota,bzip2,iperf3,pciutils,fio,psmisc,expect,perf,nvme-cli,pciutils,fio,php-cli,php-xml,php-json,libaio-devel,blktrace,fio,nvme-cli,git,tar,rng-tools,nfs-utils,libvirt,qemu-kvm,kernel-debug,python3-pip,dracut-fips,podman,xdp-tools,openssl-devel,sos,strace"
    else
        pkgs="make,automake,autoconf,sysstat,gcc,unzip,wget,quota,bzip2,iperf3,pciutils,fio,psmisc,expect,perf,nvme-cli,pciutils,fio,php-cli,php-xml,php-json,libaio-devel,blktrace,fio,nvme-cli,git,tar,rng-tools,nfs-utils,libvirt,qemu-kvm,kernel-debug,python3,dracut-fips,podman,xdp-tools,openssl-devel,sos,strace"
    fi
    if [[ ${ARCH} =~ 'aarch64' ]]; then
        waitseconds=800
        instance_type='t4g.medium'
    else
        waitseconds=600
        instance_type='t3.small'
    fi
    if [[ $COMPOSE_ID =~ '.n.' ]]; then
        echo "It is nightly build, wait ${waitseconds}s to make sure repo ready!"
        sleep ${waitseconds}
    fi
    tmp_log=$(mktemp)
    echo "Temp update log: $tmp_log"
    repo_url=$REPO_URL
    source /home/ec2/ec2_venv/bin/activate
    cd /home/ec2/mini_utils/
    build_cmd="python ec2_ami_build.py --profile ${EC2_PROFILE} --ami-id $ami_id  --key_name ${KEY_NAME} --security_group_ids ${EC2_SG_GROUP} \
        --region ${EC2_REGION} --subnet_id ${EC2_SUBNET} --tag ${VM_PREFIX}_${COMPOSE_ID}_${ARCH} --user $ssh_user --keyfile ${KEYFILE} \
        --proxy_url ${PROXY_URL} --instance_type $instance_type"
    cmd='uname -r'
    if ${UPDATE_BASEAMI}; then
        cmd=${POST_CMDS}
        if ${IS_INSTALL_PKG_LIST}; then
            build_cmd="${build_cmd} --pkgs $pkgs"
        fi
    fi

    if ! ${UPDATE_BASEAMI}; then
        new_ami=$ami_id
        echo "Use baseami $new_ami directly in testing"
    elif ! [ -z $JOB_INFO_BUILD_ID ]; then
        $build_cmd --pkg_url $PKG_URL  > $tmp_log 2>&1
    elif [[ -z $PKG_URL && $COMPOSEID_URL != "UNSPECIFIED" ]]; then
        $build_cmd --repo_url $repo_url --cmds "$cmd" > $tmp_log 2>&1
    elif [[ -z $PKG_URL && $COMPOSEID_URL == "UNSPECIFIED" ]]; then
        $build_cmd --cmds "$cmd"  > $tmp_log 2>&1
    else
        $build_cmd --repo_url $repo_url --pkg_url $PKG_URL --cmds "$cmd"  > $tmp_log 2>&1
    fi

    deactivate
    if ${UPDATE_BASEAMI}; then
        cat $tmp_log
        new_ami=$(cat $tmp_log| grep 'New AMI:'|awk -F':' '{print $NF}')
        echo "Use $new_ami for test"
    fi
    echo "IMAGE=$new_ami" >> $WORKSPACE/job_env.txt
    echo "IMAGE: $new_ami" >> $WORKSPACE/job_env.yaml
    BUILDNAME="${COMPOSE_ID}_${new_ami}"
    echo "COMPOSE_ID: $COMPOSE_ID" >> $WORKSPACE/job_env.yaml
    echo "BUILDNAME=$BUILDNAME" >> $WORKSPACE/job_env.txt
    echo "BUILDNAME: $BUILDNAME" >> $WORKSPACE/job_env.yaml
    '''
}