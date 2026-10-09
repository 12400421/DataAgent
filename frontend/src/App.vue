<template>
  <div class="app-shell">
    <header class="app-header">
      <div class="header-content">
        <RouterLink class="brand" to="/">
          <span class="brand-mark">DA</span>
          <span>
            <strong>DataAgent</strong>
            <small>数据分析工作台</small>
          </span>
        </RouterLink>
      </div>
    </header>

    <main class="page-container">
      <RouterView />
    </main>
  </div>
</template>
