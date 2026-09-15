const hre = require("hardhat");

async function main() {
  console.log("Deploying EHealthShieldACL smart contract...");

  const EHealthShieldACL = await hre.ethers.getContractFactory("EHealthShieldACL");
  const contract = await EHealthShieldACL.deploy();

  await contract.waitForDeployment();
  const address = await contract.getAddress();

  console.log(`EHealthShieldACL deployed successfully to: ${address}`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
