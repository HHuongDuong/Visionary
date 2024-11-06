// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2024-04-03',
  devtools: { enabled: true },
  css: ["~/assets/tailwind.css"],
  modules: ['@nuxtjs/tailwindcss', '@formkit/auto-animate/nuxt'],
  routeRules: {
    '/document_recognition': {
      proxy: 'http://127.0.0.1:8000/document_recognition',
    },
    '/face_detection/recognize': {
      proxy: 'http://127.0.0.1:8000/face_detection/recognize',
    },
    '/image_captioning': {
      proxy: 'http://127.0.0.1:8000/image_captioning',
    },
    '/product_recognition': {
      proxy: 'http://127.0.0.1:8000/product_recognition',
    },
    '/distance_estimate': {
      proxy: 'http://127.0.0.1:8000/distance_estimate',
    }
  }
})