import {PageData, TransformPageContext, defineConfig} from 'vitepress';
import {tabsMarkdownPlugin} from 'vitepress-plugin-tabs'
import {applySEO} from './seo';

// https://vitepress.dev/reference/site-config
export default defineConfig({
  lang: 'en-US',
  title: 'Stonecutter',
  description: 'Modern Gradle plugin for multi-version management',
  cleanUrls: true,
  appearance: 'dark',

  head: [[
    'link',
    {rel: 'icon', sizes: '32x32', href: '/assets/logo.webp'},
  ]],

  // @ts-ignore
  transformPageData: (pageData: PageData, _ctx: TransformPageContext) => {
    applySEO(pageData);
  },

  themeConfig: {
    // https://vitepress.dev/reference/default-theme-config
    logo: "/assets/logo.webp",

    nav: [
      {text: 'Home', link: '/'},
      {text: 'Wiki', link: '/wiki'},
      {text: 'KDoc', link: '/dokka', target: '_self'},
    ],

    outline: {
      level: "deep"
    },

    search: {
      provider: 'local'
    },

    sidebar: [
      {
        text: "Frequent questions", link: "/wiki/faq"
      },
      {
        text: "Getting started",
        link: "/wiki/start",
        items: [
          {text: "Project settings", link: "/wiki/start/settings"},
          {text: "Project controller", link: "/wiki/start/controller"},
          {text: "Mod setup", link: "/wiki/start/build"},
          {text: "Versioning code", link: "/wiki/start/comments"},
        ]
      },
      {
        text: "Additional config",
        items: [
          {text: "Processor parameters", link: "/wiki/config/parameters"},
          {text: "Data-driven projects", link: "/wiki/config/projects"},
          {text: "Project branches"},
        ]
      }
    ],

    socialLinks: [
      {icon: 'github', link: 'https://github.com/stonecutter-versioning/stonecutter'},
      {icon: 'discord', link: 'https://discord.gg/TBgNUCfryS'},
    ],

    footer: {
      message: 'Released under the <a href="https://github.com/stonecutter-versioning/stonecutter/blob/0.5/LICENSE">LGPL-3.0 License</a> by KikuGie (git.kikugie@protonmail.com she/her)'
    }
  },
  sitemap: {
    hostname: "https://stonecutter.kikugie.dev/",
    transformItems: items => removeVersionedItems(items)
  },
  markdown: {
    config(md) {
      // @ts-ignore
      md.use(tabsMarkdownPlugin)
    }
  }
})
