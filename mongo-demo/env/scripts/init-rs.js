function sleep(ms) { const t = Date.now(); while (Date.now() - t < ms) {} }

function waitForPrimary() {
    for (let i = 0; i < 90; i++) {
        try {
            const s = rs.status();
            const p = s.members && s.members.find(m => m.stateStr === "PRIMARY");
            if (p) return true;
        } catch (e) {}
        sleep(1000);
    }
    return false;
}

// 1) initiate（如果已初始化则跳过）
try {
    const st = rs.status();
    if (st.ok === 1) {
        print("Replica set already initiated.");
    }
} catch (e) {
    print("Initiating replica set...");
    rs.initiate({
        _id: "rs0",
        members: [
            { _id: 0, host: "mongo1:27017" },
            { _id: 1, host: "mongo2:27017" },
            { _id: 2, host: "mongo3:27017" }
        ]
    });
}

if (!waitForPrimary()) {
    throw new Error("PRIMARY not elected in time");
}

// 2) 创建 root 用户（如果不存在才创建）
// 利用 localhost exception：在没有任何用户时，即使 --auth 也允许本地创建第一个用户
db = db.getSiblingDB("admin");
const existed = db.getUser("root");
if (existed) {
    print("User root already exists.");
} else {
    print("Creating root user...");
    db.createUser({
        user: "root",
        pwd: "root123456",
        roles: [ { role: "root", db: "admin" } ]
    });
    print("Root user created.");
}
