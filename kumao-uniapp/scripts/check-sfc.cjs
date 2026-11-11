const fs = require('fs')
const path = require('path')
const { parse, compileTemplate, compileScript } = require('D:/kumao/kumao-web/node_modules/@vue/compiler-sfc')

const root = 'D:/kumao/kumao-uniapp/src'
const files = []

function walk(dir) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) walk(full)
    else if (entry.name.endsWith('.vue')) files.push(full)
  }
}
walk(root)

let errors = 0
for (const file of files) {
  const source = fs.readFileSync(file, 'utf8')
  const rel = path.relative(root, file)
  const { descriptor, errors: parseErrors } = parse(source, { filename: file })
  if (parseErrors && parseErrors.length) {
    errors += parseErrors.length
    console.log('PARSE', rel, parseErrors.map((e) => e.message).join('; '))
    continue
  }
  try {
    if (descriptor.script || descriptor.scriptSetup) {
      compileScript(descriptor, { id: rel })
    }
  } catch (e) {
    errors++
    console.log('SCRIPT', rel, e.message)
  }
  if (descriptor.template) {
    const res = compileTemplate({
      source: descriptor.template.content,
      filename: file,
      id: rel
    })
    if (res.errors && res.errors.length) {
      errors += res.errors.length
      console.log('TEMPLATE', rel, res.errors.map((e) => e.message || e).join('; '))
    }
  }
}
console.log(`checked ${files.length} files, errors: ${errors}`)

