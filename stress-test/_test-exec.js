const { execSync } = require('child_process');
try {
  execSync('"C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysql.exe" --host=127.0.0.1 --user=root --password=123456 --database=parking_system --execute="SELECT 1;"', { stdio: 'ignore' });
  console.log('execSync stdio=ignore: OK');
} catch (e) {
  console.log('execSync failed:', e.code || e.message);
}
