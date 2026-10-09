import { createRouter, createWebHistory } from 'vue-router'

import DatasetDetailView from '../views/DatasetDetailView.vue'
import DatasetListView from '../views/DatasetListView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'datasets',
      component: DatasetListView,
    },
    {
      path: '/datasets/:id',
      name: 'dataset-detail',
      component: DatasetDetailView,
      props: true,
    },
  ],
})

export default router
