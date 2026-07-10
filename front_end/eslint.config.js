import js from '@eslint/js'
import vue from 'eslint-plugin-vue'

export default [
  js.configs.recommended,
  ...vue.configs['flat/recommended'],
  {
    languageOptions: {
      globals: {
        File: 'readonly',
        FormData: 'readonly',
        XMLHttpRequest: 'readonly',
        Blob: 'readonly',
        localStorage: 'readonly',
        sessionStorage: 'readonly',
        window: 'readonly',
        document: 'readonly',
        navigator: 'readonly',
        console: 'readonly',
        setTimeout: 'readonly',
        import: 'readonly',
        URLSearchParams: 'readonly',
        fetch: 'readonly',
        ElMessage: 'readonly',
        ElMessageBox: 'readonly',
        Element: 'readonly',
        Node: 'readonly',
        FileReader: 'readonly',
        HTMLElement: 'readonly',
        HTMLInputElement: 'readonly',
        HTMLAnchorElement: 'readonly'
      }
    },
    rules: {
      'no-unused-vars': 'warn',
      'no-console': 'warn',
      'no-undef': 'error',
      'vue/multi-word-component-names': 'off',
      'vue/no-unused-vars': 'warn'
    }
  },
  {
    ignores: ['dist/**', 'node_modules/**', 'release/**', 'electron/**']
  }
]
