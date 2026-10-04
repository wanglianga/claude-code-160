<template>
  <router-view />
</template>

<style>
* { box-sizing: border-box; }
body { margin: 0; font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif; background: #f0f2f5; }
#app { height: 100vh; }
.page-card { background: #fff; border-radius: 8px; padding: 16px; margin-bottom: 16px; }
.page-title { font-size: 16px; font-weight: 600; margin-bottom: 12px; color: #303133; display: flex; align-items: center; gap: 8px; }
.toolbar { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; margin-bottom: 12px; }
.stat-num { font-size: 28px; font-weight: 700; }
</style>
