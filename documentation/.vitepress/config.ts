import { defineConfig } from 'vitepress'

export default defineConfig({
  title: 'Podcast Server',
  description: 'Self-host every podcast in your home network',

  base: '/',

  lastUpdated: true,

  head: [
    ['link', { rel: 'icon', type: 'image/svg+xml', href: '/favicon.svg' }],
  ],

  themeConfig: {
    logo: {
      light: '/logo-light.svg',
      dark: '/logo-dark.svg',
    },

    nav: [
      { text: 'Get started', link: '/install/production' },
      { text: 'GitLab', link: 'https://gitlab.com/davinkevin/Podcast-Server' },
    ],

    sidebar: [
      {
        text: 'Get started',
        items: [
          { text: 'Deploy to production', link: '/install/production' },
          { text: 'Local dev setup', link: '/install/local' },
        ],
      },
    ],

    socialLinks: [
      { icon: 'github', link: 'https://github.com/davinkevin/Podcast-Server' },
    ],

    footer: {
      message: 'Released under the Apache 2.0 License.',
      copyright: '© Davin Kevin',
    },
  },
})
