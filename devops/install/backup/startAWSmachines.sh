echo "start Report Portal"
aws ec2 start-instances --instance-ids $(aws ec2 describe-instances --filters "Name=tag:Name,Values=Report Portal" --query "Reservations[].Instances[].InstanceId" --output text)
                    
echo "start Jenkins Worker 1"
aws ec2 start-instances --instance-ids $(aws ec2 describe-instances --filters "Name=tag:Name,Values=Jenkins Worker 1" --query "Reservations[].Instances[].InstanceId" --output text)

echo "start Jenkins server"
aws ec2 start-instances --instance-ids $(aws ec2 describe-instances --filters "Name=tag:Name,Values=Jenkins Server" --query "Reservations[].Instances[].InstanceId" --output text)