import User from '../pages/User.vue'
import Index from '../pages/Index.vue'
import Team from '../pages/Team.vue'
import Search from '../pages/Search.vue'
import Edit from '../pages/Edit.vue'



const routes = [
    { path: '/user', component: User },
    { path: '/', component: Index },
    { path: '/team', component: Team },
    { path: '/search', component: Search },
    { path: '/user/edit', component: Edit },
]

export default routes;

